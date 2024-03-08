package me.fexus.audio.openal

import org.lwjgl.openal.AL10.*


fun <T>T.catchALError(): T {
    me.fexus.audio.openal.catchALError()
    return this
}

fun catchALError() {
    val errCode = alGetError()

    if (errCode == AL_NO_ERROR) return

    val errString = when (errCode) {
        40961 -> "Invalid name"
        40962 -> "Invalid enum"
        40963 -> "Invalid value"
        40964 -> "Invalid Operation"
        40965 -> "Out of memory"
        else -> "Unknown"
    }

    throw Exception("AL Error: $errString")
}