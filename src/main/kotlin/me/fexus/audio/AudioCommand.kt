package me.fexus.audio

import java.util.concurrent.atomic.AtomicBoolean


interface AudioCommand<R> {
    val isExecuted: AtomicBoolean
    var result: R
}