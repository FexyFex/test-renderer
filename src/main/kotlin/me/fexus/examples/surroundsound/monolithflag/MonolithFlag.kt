package me.fexus.examples.surroundsound.monolithflag

enum class MonolithFlag(override val bits: Int): MonolithFlags {
    NONE(0),
    NEAR_TRIGGER(1),
    TRIGGERED(2),
    HUMMMING(4)
}