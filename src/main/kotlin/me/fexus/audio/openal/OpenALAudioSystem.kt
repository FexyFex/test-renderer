package me.fexus.audio.openal


object OpenALAudioSystem {
    /*
    private val openALDevice = alcOpenDevice(null as ByteBuffer?)
    private val openALContext = alcCreateContext(openALDevice, null as IntBuffer?)
    private val capabilities = createCapabilities(openALDevice)


    init {
        if (openALDevice == 0L) throw IllegalStateException("Failed to open the default OpenAL device")
        if (openALContext == 0L) throw IllegalStateException("Failed to create OpenAL context")
        alcMakeContextCurrent(openALContext)
        AL.createCapabilities(capabilities)
    }


    fun loadAudioDataFromResource(filePath: String): DefaultAudioFileData {
        val stream = ClassLoader.getSystemResourceAsStream(filePath)!!
        val fileFormat = FileFormat.values().first { it.extension == filePath.substringAfterLast(".") }
        val decoder = fileFormat.decoder.constructors.first().call() as AudioFileDecoder
        val header = decoder.readHeader(stream)
        val bytes = decoder.readData(header, stream)
        return DefaultAudioFileData(header, bytes.toBuffoon())
    }


    fun createSoundBuffer(audioData: DefaultAudioFileData): AudioBufferTrack {
        val soundBuffer = OpenALAudioBuffer()
        alBufferData(soundBuffer.bufferID, audioData.openALFormat, audioData.data, audioData.header.sampleRate)
        catchALError()
        return AudioBufferTrack(soundBuffer, audioData)
    }


    fun destroy() {
        alcDestroyContext(openALContext)
        alcCloseDevice(openALDevice)
    }
     */
}