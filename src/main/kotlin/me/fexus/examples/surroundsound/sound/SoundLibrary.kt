package me.fexus.examples.surroundsound.sound

import me.fexus.audio.AudioFileFormat


enum class SoundLibrary(override val pathInResources: String, override val audioFileFormat: AudioFileFormat): Sound {
    SUCCESS_SOUND("audio/success.ogg", AudioFileFormat.OGG),
    THE_SLEEPING_SEA("audio/the_sleeping_sea.ogg", AudioFileFormat.OGG),
    FIRST_REVOLUTION("audio/first_revolution.ogg", AudioFileFormat.OGG)
}