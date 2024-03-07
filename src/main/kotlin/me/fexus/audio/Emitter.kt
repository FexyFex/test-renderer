package me.fexus.audio

import javax.sound.sampled.Mixer


interface Emitter {
    val mixer: Mixer
}