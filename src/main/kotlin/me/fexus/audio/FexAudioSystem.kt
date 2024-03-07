package me.fexus.audio

class FexAudioSystem {
    companion object {
        val DEFAULT_CONFIGURATION = AudioSystemConfiguration(
            "audio/"
        )
    }

    lateinit var config: AudioSystemConfiguration; private set

    fun init(config: AudioSystemConfiguration = DEFAULT_CONFIGURATION): FexAudioSystem {


        return this
    }


    fun close() {

    }
}