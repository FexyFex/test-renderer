package me.fexus.audio.decoder

import com.jcraft.jogg.Packet
import com.jcraft.jogg.Page
import com.jcraft.jogg.StreamState
import com.jcraft.jogg.SyncState
import com.jcraft.jorbis.Block
import com.jcraft.jorbis.Comment
import com.jcraft.jorbis.DspState
import com.jcraft.jorbis.Info
import me.fexus.audio.AudioBuffer
import me.fexus.audio.AudioDataDecoder
import me.fexus.math.clamp
import java.io.InputStream
import javax.sound.sampled.AudioFormat
import kotlin.math.min


class AudioDecoderOGG(override val audioStream: InputStream): AudioDataDecoder {
    private lateinit var buffer: ByteArray
    private var bufferSize: Int = 2048
    private var count = 0
    private var index = 0

    private lateinit var convertedBuffer: ByteArray
    private var convertedBufferSize = 0

    // Here are the four required JOgg objects...
    private val joggPacket: Packet = Packet()
    private val joggPage: Page = Page()
    private val joggStreamState = StreamState()
    private val joggSyncState = SyncState()

    // ... followed by the four required JOrbis objects.
    private val jorbisDspState = DspState()
    private val jorbisBlock: Block = Block(jorbisDspState)
    private val jorbisComment: Comment = Comment()
    private val jorbisInfo: Info = Info()

    private lateinit var pcmInfo: Array<Array<FloatArray?>?>
    private lateinit var pcmIndex: IntArray

    private val unpackedAudioData = ArrayList<Byte>()
    private var unpackedAudioDataOffset = 0
    override val audioDataSize: Int; get() = unpackedAudioData.size

    override lateinit var audioFormat: AudioFormat; private set
    override val isInitialized: Boolean; get() = this::audioFormat.isInitialized


    override fun init(): AudioDecoderOGG {
        joggSyncState.init()
        joggSyncState.buffer(bufferSize)
        buffer = joggSyncState.data

        val fileValid = readHeader()
        if (!fileValid) throw InvalidOGGDataException()

        initSound()

        this.audioFormat = AudioFormat(jorbisInfo.rate.toFloat(), 16, jorbisInfo.channels, true, false)
        this.pcmInfo = arrayOfNulls(1)
        this.pcmIndex = IntArray(jorbisInfo.channels)

        readBody()
        //unpackedAudioDataOffset = 0

        return this
    }

    private fun initSound() {
        convertedBufferSize = bufferSize * 2
        convertedBuffer = ByteArray(convertedBufferSize)

        jorbisDspState.synthesis_init(jorbisInfo)
        jorbisBlock.init(jorbisDspState)
    }

    private fun readHeader(): Boolean {
        var needMoreData = true
        var currentPacket = 1

        while (needMoreData) {
            count = audioStream.read(buffer, index, bufferSize)
            joggSyncState.wrote(count)

            if (currentPacket == 1) {
                val pageOut = joggSyncState.pageout(joggPage)
                if (pageOut == -1) return false
                if (pageOut == 1) {
                    joggStreamState.init(joggPage.serialno())
                    joggStreamState.reset()

                    jorbisInfo.init()
                    jorbisComment.init()

                    if (joggStreamState.pagein(joggPage) == -1) return false
                    if (joggStreamState.packetout(joggPacket) != 1) return false
                    if (jorbisInfo.synthesis_headerin(jorbisComment, joggPacket) < 0) return false

                    currentPacket++
                }
                if (currentPacket == 1) continue
            }
            if (currentPacket == 2 || currentPacket == 3) {
                val pageOut = joggSyncState.pageout(joggPage)
                if (pageOut == -1) return false
                if (pageOut == 1) {
                    joggStreamState.pagein(joggPage)

                    val packetOut = joggStreamState.packetout(joggPacket)
                    if (packetOut == -1) return false
                    if (packetOut == 1) {
                        jorbisInfo.synthesis_headerin(jorbisComment, joggPacket)

                        currentPacket++
                        if (currentPacket == 4) needMoreData = false
                    }
                }
            }

            this.index = joggSyncState.buffer(bufferSize)
            this.buffer = joggSyncState.data

            if (count == 0 && needMoreData) return false
        }

        return true
    }

    private fun readBody() {
        var needMoreData = true

        var i = -1
        while (needMoreData) {
            i++
            val pageOut = joggSyncState.pageout(joggPage)
            if (pageOut == 1) {
                joggStreamState.pagein(joggPage)

                if (joggPage.granulepos() > 0L) {
                    processPackets@ while (true) {
                        val packetOut = joggStreamState.packetout(joggPacket)
                        if (packetOut == 1) decodeCurrentPacket() else break@processPackets
                    }
                    if (joggPage.eos() != 0) needMoreData = false
                } else {
                    needMoreData = false
                }
            }

            if (needMoreData) {
                this.index = joggSyncState.buffer(this.bufferSize)
                this.buffer = joggSyncState.data

                val actualIndex = if (index == -1) 0 else index
                this.count = audioStream.read(buffer, actualIndex, bufferSize)

                joggSyncState.wrote(count)

                if (count == 0 || index == -1) needMoreData = false
            }
        }
    }

    private fun decodeCurrentPacket() {
        if (jorbisBlock.synthesis(joggPacket) == 0)
            jorbisDspState.synthesis_blockin(jorbisBlock)

        var samples: Int = jorbisDspState.synthesis_pcmout(pcmInfo, pcmIndex)
        while (samples > 0) {
            val range = if (samples < convertedBufferSize) samples else convertedBufferSize

            for (i in 0 until jorbisInfo.channels) {
                var sampleIndex = i * 2

                for (j in 0 until range) {
                    var value = (pcmInfo[0]!![i]!![pcmIndex[i] + j] * 32767).toInt().clamp(-32768, 32767)

                    if (value < 0) value = value or 32768

                    convertedBuffer[sampleIndex] = value.toByte()
                    convertedBuffer[sampleIndex + 1] = (value ushr 8).toByte()

                    sampleIndex += 2 * (jorbisInfo.channels)
                }
            }

            val length = 2 * jorbisInfo.channels * range
            for (i in 0 until length) {
                unpackedAudioData.add(unpackedAudioDataOffset++, convertedBuffer[i])
            }

            jorbisDspState.synthesis_read(range)

            samples = jorbisDspState.synthesis_pcmout(pcmInfo, pcmIndex)
        }
    }

    override fun getFullAudioData(): AudioBuffer {
        return AudioBuffer(unpackedAudioData.toByteArray(), this.audioFormat)
    }

    override fun getAudioData(offset: Int, size: Int): AudioBuffer {
        val start = offset.clamp(0, unpackedAudioData.size)
        val end = (offset + size).clamp(offset, unpackedAudioData.size)
        val slice = unpackedAudioData.subList(start, end).toByteArray()

        return AudioBuffer(slice, this.audioFormat)
    }


    class InvalidOGGDataException: Exception()
}