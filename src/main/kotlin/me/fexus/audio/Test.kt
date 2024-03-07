package me.fexus.audio

import me.fexus.audio.decoder.AudioDecoderOGG
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine


fun main() {
    val audioSystem = FexAudioSystem().init()

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