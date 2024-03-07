package me.fexus.audio.decoder

import com.jcraft.jogg.Packet
import com.jcraft.jogg.Page
import com.jcraft.jogg.StreamState
import com.jcraft.jogg.SyncState
import com.jcraft.jorbis.Block
import com.jcraft.jorbis.Comment
import com.jcraft.jorbis.DspState
import com.jcraft.jorbis.Info
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.net.URLConnection
import java.net.UnknownServiceException
import javax.sound.sampled.*

/*
 * Copyright &copy; Jon Kristensen, 2008.
 * All rights reserved.
 *
 * This is version 1.0 of this source code, made to work with JOrbis 1.x. The
 * last time this file was updated was the 15th of March, 2008.
 *
 * Version history:
 *
 * 1.0: Initial release.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   * Neither the name of jonkri.com nor the names of its contributors may be
 *     used to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */



/**
 * The `ExamplePlayer` thread class will simply download and play
 * OGG media. All you need to do is supply a valid URL as the first argument.
 *
 * @author Jon Kristensen
 * @version 1.0
 */
class ExampleOGGDecoderAndPlayer internal constructor(pUrl: String) : Thread() {
    // If you wish to debug this source, please set the variable below to true.
    private val debugMode = false

    /*
	 * URLConnection and InputStream objects so that we can open a connection to
	 * the media file.
	 */
    private var urlConnection: URLConnection? = null
    private lateinit var inputStream: InputStream

    /*
	 * We need a buffer, it's size, a count to know how many bytes we have read
	 * and an index to keep track of where we are. This is standard networking
	 * stuff used with read().
	 */
    var buffer: ByteArray? = null
    var bufferSize = 2048
    var count = 0
    var index = 0

    /*
	 * JOgg and JOrbis require fields for the converted buffer. This is a buffer
	 * that is modified in regards to the number of audio channels. Naturally,
	 * it will also need a size.
	 */
    lateinit var convertedBuffer: ByteArray
    var convertedBufferSize = 0

    // The source data line onto which data can be written.
    private var outputLine: SourceDataLine? = null

    // A three-dimensional an array with PCM information.
    private lateinit var pcmInfo: Array<Array<FloatArray?>?>

    // The index for the PCM information.
    private lateinit var pcmIndex: IntArray

    // Here are the four required JOgg objects...
    private val joggPacket = Packet()
    private val joggPage = Page()
    private val joggStreamState = StreamState()
    private val joggSyncState = SyncState()

    // ... followed by the four required JOrbis objects.
    private val jorbisDspState = DspState()
    private val jorbisBlock = Block(jorbisDspState)
    private val jorbisComment = Comment()
    private val jorbisInfo = Info()

    /**
     * The constructor; will configure the `InputStream`.
     *
     * @param pUrl the URL to be opened
     */
    init {
        configureInputStream(getUrl(pUrl))
    }

    /**
     * Given a string, `getUrl()` will return an URL object.
     *
     * @param pUrl the URL to be opened
     * @return the URL object
     */
    fun getUrl(pUrl: String): URL {
        var url: URL
        try {
            url = URL(pUrl)
        } catch (exception: MalformedURLException) {
            System.err.println("Malformed \"url\" parameter: \"$pUrl\"")
            url = URL(pUrl)
        }
        return url
    }

    /**
     * Sets the `inputStream` object by taking an URL, opens a
     * connection to it and get the `InputStream`.
     *
     * @param pUrl the url to the media file
     */
    private fun configureInputStream(pUrl: URL) {
        val stream = pUrl.readBytes().inputStream()
        this.inputStream = stream
    }

    /**
     * This method is probably easiest understood by looking at the body.
     * However, it will - if no problems occur - call methods to initialize the
     * JOgg JOrbis libraries, read the header, initialize the sound system, read
     * the body of the stream and clean up.
     */
    override fun run() {
        // Initialize JOrbis.
        initializeJOrbis()

        /*
		 * If we can read the header, we try to inialize the sound system. If we
		 * could initialize the sound system, we try to read the body.
		 */if (readHeader()) {
            if (initializeSound()) {
                readBody()
            }
        }

        // Afterwards, we clean up.
        cleanUp()
    }

    /**
     * Initializes JOrbis. First, we initialize the `SyncState`
     * object. After that, we prepare the `SyncState` buffer. Then
     * we "initialize" our buffer, taking the data in `SyncState`.
     */
    private fun initializeJOrbis() {
        debugOutput("Initializing JOrbis.")

        // Initialize SyncState
        joggSyncState.init()

        // Prepare the to SyncState internal buffer
        joggSyncState.buffer(bufferSize)

        /*
		 * Fill the buffer with the data from SyncState's internal buffer. Note
		 * how the size of this new buffer is different from bufferSize.
		 */
        buffer = joggSyncState.data
        debugOutput("Done initializing JOrbis.")
    }

    /**
     * This method reads the header of the stream, which consists of three
     * packets.
     *
     * @return true if the header was successfully read, false otherwise
     */
    private fun readHeader(): Boolean {
        debugOutput("Starting to read the header.")

        /*
		 * Variable used in loops below. While we need more data, we will
		 * continue to read from the InputStream.
		 */
        var needMoreData = true

        /*
		 * We will read the first three packets of the header. We start off by
		 * defining packet = 1 and increment that value whenever we have
		 * successfully read another packet.
		 */
        var packet = 1

        /*
		 * While we need more data (which we do until we have read the three
		 * header packets), this loop reads from the stream and has a big
		 * <code>switch</code> statement which does what it's supposed to do in
		 * regard to the current packet.
		 */
        while (needMoreData) {
            // Read from the InputStream.
            try {
                count = inputStream.read(buffer!!, index, bufferSize)
            } catch (exception: IOException) {
                System.err.println("Could not read from the input stream.")
                System.err.println(exception)
            }

            // We let SyncState know how many bytes we read.
            joggSyncState.wrote(count)
            when (packet) {
                1 -> {
                    when (joggSyncState.pageout(joggPage)) {
                        -1 -> {
                            System.err.println("There is a hole in the first packet data.")
                            return false
                        }

                        0 -> {}
                        1 -> {

                            // Initializes and resets StreamState.
                            joggStreamState.init(joggPage.serialno())
                            joggStreamState.reset()

                            // Initializes the Info and Comment objects.
                            jorbisInfo.init()
                            jorbisComment.init()

                            // Check the page (serial number and stuff).
                            if (joggStreamState.pagein(joggPage) == -1) {
                                System.err.println(
                                    "We got an error while "
                                            + "reading the first header page."
                                )
                                return false
                            }

                            /*
                             * Try to extract a packet. All other return values
                             * than "1" indicates there's something wrong.
                             */if (joggStreamState.packetout(joggPacket) != 1) {
                                System.err.println(
                                    "We got an error while "
                                            + "reading the first header packet."
                                )
                                return false
                            }

                            /*
                             * We give the packet to the Info object, so that it
                             * can extract the Comment-related information,
                             * among other things. If this fails, it's not
                             * Vorbis data.
                             */if (jorbisInfo.synthesis_headerin(
                                    jorbisComment,
                                    joggPacket
                                ) < 0
                            ) {
                                System.err.println(
                                    "We got an error while "
                                            + "interpreting the first packet. "
                                            + "Apparantly, it's not Vorbis data."
                                )
                                return false
                            }

                            // We're done here, let's increment "packet".
                            packet++
                        }

                        else -> {}
                    }

                    /*
                     * Note how we are NOT breaking here if we have proceeded to
                     * the second packet. We don't want to read from the input
                     * stream again if it's not necessary.
                     */
                    //if (packet == 1) return@run

                    when (joggSyncState.pageout(joggPage)) {
                        -1 -> {
                            System.err.println(
                                "There is a hole in the second "
                                        + "or third packet data."
                            )
                            return false
                        }
                        1 -> {
                            // Share the page with the StreamState object.
                            joggStreamState.pagein(joggPage)
                            when (joggStreamState.packetout(joggPacket)) {
                                -1 -> {
                                    System.err
                                        .println(
                                            "There is a hole in the first"
                                                    + "packet data."
                                        )
                                    return false
                                }
                                1 -> {

                                    /*
                                 * Like above, we give the packet to the
                                 * Info and Comment objects.
                                 */
                                    jorbisInfo.synthesis_headerin(
                                        jorbisComment, joggPacket
                                    )

                                    // Increment packet.
                                    packet++
                                    if (packet == 4) {
                                        /*
                                     * There is no fourth packet, so we will
                                     * just end the loop here.
                                     */
                                        needMoreData = false
                                    }
                                }
                            }
                        }
                    }
                }

                2, 3 -> {
                    when (joggSyncState.pageout(joggPage)) {
                        -1 -> return false
                        1 -> {
                            joggStreamState.pagein(joggPage)
                            when (joggStreamState.packetout(joggPacket)) {
                                -1 -> {
                                    System.err
                                        .println(
                                            "There is a hole in the first"
                                                    + "packet data."
                                        )
                                    return false
                                }

                                0 -> {}
                                1 -> {
                                    jorbisInfo.synthesis_headerin(
                                        jorbisComment, joggPacket
                                    )
                                    packet++
                                    if (packet == 4) {
                                        needMoreData = false
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // We get the new index and an updated buffer.
            index = joggSyncState.buffer(bufferSize)
            buffer = joggSyncState.data

            /*
			 * If we need more data but can't get it, the stream doesn't contain
			 * enough information.
			 */
            if (count == 0 && needMoreData) {
                System.err.println("Not enough header data was supplied.")
                return false
            }
        }
        debugOutput("Finished reading the header.")
        return true
    }

    /**
     * This method starts the sound system. It starts with initializing the
     * `DspState` object, after which it sets up the
     * `Block` object. Last but not least, it opens a line to the
     * source data line.
     *
     * @return true if the sound system was successfully started, false
     * otherwise
     */
    private fun initializeSound(): Boolean {
        debugOutput("Initializing the sound system.")

        // This buffer is used by the decoding method.
        convertedBufferSize = bufferSize * 2
        convertedBuffer = ByteArray(convertedBufferSize)

        // Initializes the DSP synthesis.
        jorbisDspState.synthesis_init(jorbisInfo)

        // Make the Block object aware of the DSP.
        jorbisBlock.init(jorbisDspState)

        // Wee need to know the channels and rate.
        val channels = jorbisInfo.channels
        val rate = jorbisInfo.rate

        // Creates an AudioFormat object and a DataLine.Info object.
        val audioFormat = AudioFormat(
            rate.toFloat(), 16, channels,
            true, false
        )
        val datalineInfo = DataLine.Info(
            SourceDataLine::class.java,
            audioFormat, AudioSystem.NOT_SPECIFIED
        )

        // Check if the line is supported.
        if (!AudioSystem.isLineSupported(datalineInfo)) {
            System.err.println("Audio output line is not supported.")
            return false
        }

        /*
		 * Everything seems to be alright. Let's try to open a line with the
		 * specified format and start the source data line.
		 */try {
            outputLine = AudioSystem.getLine(datalineInfo) as SourceDataLine
            outputLine!!.open(audioFormat)
        } catch (exception: LineUnavailableException) {
            println("The audio output line could not be opened due to resource restrictions.")
            System.err.println(exception)
            return false
        } catch (exception: IllegalStateException) {
            println("The audio output line is already open.")
            System.err.println(exception)
            return false
        } catch (exception: SecurityException) {
            println(
                "The audio output line could not be opened due "
                        + "to security restrictions."
            )
            System.err.println(exception)
            return false
        }

        // Start it.
        outputLine!!.start()

        /*
		 * We create the PCM variables. The index is an array with the same
		 * length as the number of audio channels.
		 */
        pcmInfo = arrayOfNulls(1)
        pcmIndex = IntArray(jorbisInfo.channels)
        debugOutput("Done initializing the sound system.")
        return true
    }

    /**
     * This method reads the entire stream body. Whenever it extracts a packet,
     * it will decode it by calling `decodeCurrentPacket()`.
     */
    private fun readBody() {
        debugOutput("Reading the body.")

        /*
		 * Variable used in loops below, like in readHeader(). While we need
		 * more data, we will continue to read from the InputStream.
		 */
        var needMoreData = true
        while (needMoreData) {
            when (joggSyncState.pageout(joggPage)) {
                1 -> {
                    // Give the page to the StreamState object.
                    joggStreamState.pagein(joggPage)

                    // If granulepos() returns "0", we don't need more data.
                    if (joggPage.granulepos() == 0L) {
                        needMoreData = false
                    }

                    // Here is where we process the packets.
                    processPackets@ while (true) {
                        when (joggStreamState.packetout(joggPacket)) {
                            -1 -> {
                                debugOutput("There is a hole in the data, we continue though.")
                                break@processPackets
                            }
                            0 -> break@processPackets
                            1 -> decodeCurrentPacket()
                        }
                    }

                    /*
					 * If the page is the end-of-stream, we don't need more
					 * data.
					 */if (joggPage.eos() != 0) needMoreData = false
                }
            }

            // If we need more data...
            if (needMoreData) {
                // We get the new index and an updated buffer.
                index = joggSyncState.buffer(bufferSize)
                buffer = joggSyncState.data

                // Read from the InputStream.
                count = try {
                    inputStream!!.read(buffer!!, index, bufferSize)
                } catch (e: Exception) {
                    System.err.println(e)
                    return
                }

                // We let SyncState know how many bytes we read.
                joggSyncState.wrote(count)

                // There's no more data in the stream.
                if (count == 0) needMoreData = false
            }
        }
        debugOutput("Done reading the body.")
    }

    /**
     * A clean-up method, called when everything is finished. Clears the
     * JOgg/JOrbis objects and closes the `InputStream`.
     */
    private fun cleanUp() {
        debugOutput("Cleaning up.")

        // Clear the necessary JOgg/JOrbis objects.
        joggStreamState.clear()
        jorbisBlock.clear()
        jorbisDspState.clear()
        jorbisInfo.clear()
        joggSyncState.clear()

        // Closes the stream.
        try {
            if (inputStream != null) inputStream!!.close()
        } catch (_: Exception) {
        }
        debugOutput("Done cleaning up.")
    }

    /**
     * Decodes the current packet and sends it to the audio output line.
     */
    private fun decodeCurrentPacket() {
        var samples: Int

        // Check that the packet is a audio data packet etc.
        if (jorbisBlock.synthesis(joggPacket) == 0) {
            // Give the block to the DspState object.
            jorbisDspState.synthesis_blockin(jorbisBlock)
        }

        // We need to know how many samples to process.
        var range: Int

        /*
		 * Get the PCM information and count the samples. And while these
		 * samples are more than zero...
		 */
        while (jorbisDspState.synthesis_pcmout(pcmInfo, pcmIndex).also { samples = it } > 0) {
            // We need to know for how many samples we are going to process.
            range = if (samples < convertedBufferSize) {
                samples
            } else {
                convertedBufferSize
            }

            // For each channel...
            for (i in 0 until jorbisInfo.channels) {
                var sampleIndex = i * 2

                // For every sample in our range...
                for (j in 0 until range) {
                    /*
					 * Get the PCM value for the channel at the correct
					 * position.
					 */
                    var value = (pcmInfo[0]!![i]!![pcmIndex[i] + j] * 32767).toInt()

                    /*
					 * We make sure our value doesn't exceed or falls below
					 * +-32767.
					 */
                    if (value > 32767) {
                        value = 32767
                    }
                    if (value < -32768) {
                        value = -32768
                    }

                    /*
					 * It the value is less than zero, we bitwise-or it with
					 * 32768 (which is 1000000000000000 = 10^15).
					 */if (value < 0) value = value or 32768

                    /*
					 * Take our value and split it into two, one with the last
					 * byte and one with the first byte.
					 */convertedBuffer[sampleIndex] = value.toByte()
                    convertedBuffer[sampleIndex + 1] = (value ushr 8).toByte()

                    /*
					 * Move the sample index forward by two (since that's how
					 * many values we get at once) times the number of channels.
					 */sampleIndex += 2 * jorbisInfo.channels
                }
            }

            // Write the buffer to the audio output line.
            outputLine!!.write(
                convertedBuffer, 0, 2 * jorbisInfo.channels
                        * range
            )

            // Update the DspState object.
            jorbisDspState.synthesis_read(range)
        }
    }

    /**
     * This method is being called internally to output debug information
     * whenever that is wanted.
     *
     * @param output the debug output information
     */
    private fun debugOutput(output: String) {
        if (debugMode) println("Debug: $output")
    }

    companion object {
        /**
         * The programs `main()` method. Will read the first
         * command-line argument and use it as URL, after which it will start the
         * thread.
         *
         * @param args command-line arguments
         */
        @JvmStatic
        fun main(args: Array<String>) {
            // Set the URL as the first argument, if any.
//            val url = if (args.isNotEmpty()) args[0] else null
            val url = ClassLoader.getSystemResource("audio/the_sleeping_sea.ogg").toString()
            /*
             * If the url variable is set, start the thread. If not, give an error
		     * and die.
		     */
            if (url != null) {
                val examplePlayer = ExampleOGGDecoderAndPlayer(url)
                examplePlayer.start()
            } else {
                System.err.println(
                    "Please provide an argument with the file to play."
                )
            }
        }
    }
}

