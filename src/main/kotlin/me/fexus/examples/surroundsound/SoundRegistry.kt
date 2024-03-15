package me.fexus.examples.surroundsound

import me.fexus.audio.AudioDataDecoder
import me.fexus.audio.FexAudioSystem


class SoundRegistry {
    private val loadedSounds = mutableMapOf<Sound, AudioDataDecoder>()


    fun loadSound(sound: Sound): AudioDataDecoder {
        if (loadedSounds.containsKey(sound)) return loadedSounds[sound]!!

        val fileData = ClassLoader.getSystemResource(sound.pathInResources).readBytes()
        val decoder = FexAudioSystem.createDecoder(fileData, sound.audioFileFormat)

        loadedSounds[sound] = decoder
        return decoder
    }


    fun clear() = loadedSounds.clear()
}