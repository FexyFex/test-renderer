package me.fexus.audio.decoder.ogg

class PCMInfoArray(size: Int) {
    val arr = arrayOfNulls<Array<FloatArray>>(size)

    operator fun get(x: Int, y: Int, z: Int): Float? = arr[x]?.get(y)?.get(z)
    operator fun set(x: Int, y: Int, z: Int, value: Float) { arr[x]?.get(y)?.set(z, value) }
}