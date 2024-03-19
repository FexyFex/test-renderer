package me.fexus.examples.surroundsound.sound

import me.fexus.audio.AudioFileFormat


interface Sound {
    val pathInResources: String
    val audioFileFormat: AudioFileFormat
}