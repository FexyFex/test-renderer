package me.fexus.audio

import me.fexus.audio.decoder.WAVAudioDecoder
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine


fun main() {
    val fileData = ClassLoader.getSystemResource("audio/first_revolution.wav").readBytes()
    val fileDecoder = WAVAudioDecoder(fileData)
    fileDecoder.init()

    val dataLineInfo = DataLine.Info(SourceDataLine::class.java, fileDecoder.audioFormat)
    val sourceDataLine: SourceDataLine = AudioSystem.getLine(dataLineInfo) as SourceDataLine
    sourceDataLine.open()
    sourceDataLine.start()

    while (!fileDecoder.isEndOfStream) {
        val soundBuffer = fileDecoder.getAudioData(512)
        sourceDataLine.write(soundBuffer.data, 0, soundBuffer.size)
    }

    sourceDataLine.drain()
    sourceDataLine.close()
}