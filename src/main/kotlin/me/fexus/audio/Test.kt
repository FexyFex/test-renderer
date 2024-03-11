package me.fexus.audio

import me.fexus.audio.libraries.AudioLibraryJavaAudioSystem


fun main() {
    val audioSystem = FexAudioSystem().initWithLibrary<AudioLibraryJavaAudioSystem>()

    val fileData = ClassLoader.getSystemResource("audio/success.ogg").readBytes()
    val decoder = audioSystem.createDecoder(fileData, AudioFileFormat.OGG)

    val source = audioSystem.createEmitter()
    val channel = audioSystem.createChannel(AudioChannel.Type.ALL_AT_ONCE, decoder)

    source.play(channel)
    audioSystem.shutdown()
}

/*
fun oldmain() {
    val audioSystem = FexAudioSystem().initWithLibrary<AudioLibraryJavaAudioSystem>()

    val fileData = ClassLoader.getSystemResource("audio/the_sleeping_sea.ogg").readBytes()
    val fileDecoder = AudioDecoderOGG(fileData).init()

    val dataLineInfo = DataLine.Info(SourceDataLine::class.java, fileDecoder.audioFormat)
    val sourceDataLine: SourceDataLine = AudioSystem.getLine(dataLineInfo) as SourceDataLine
    sourceDataLine.open()
    sourceDataLine.start()

    println("PLAYING")
    while (!fileDecoder.isEndOfStream) {
        val soundBuffer = fileDecoder.getAudioData(512)
        sourceDataLine.write(soundBuffer.data, 0, soundBuffer.size)
    }
    println("EXHAUSTED")

    sourceDataLine.drain()
    sourceDataLine.close()

    audioSystem.close()
}

 */