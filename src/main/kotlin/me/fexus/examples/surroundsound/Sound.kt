package me.fexus.examples.surroundsound

import me.fexus.audio.AudioFileFormat


interface Sound {
    val pathInResources: String
    val audioFileFormat: AudioFileFormat
}