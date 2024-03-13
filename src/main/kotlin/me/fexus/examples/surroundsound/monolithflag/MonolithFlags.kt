package me.fexus.examples.surroundsound.monolithflag

interface MonolithFlags {
    val bits: Int

    operator fun contains(flags: CombinedMonolithFlags): Boolean = (this.bits and flags.bits) == flags.bits

    operator fun plus(other: MonolithFlags): MonolithFlags = CombinedMonolithFlags(this.bits or other.bits)
    operator fun minus(other: MonolithFlags): MonolithFlags = CombinedMonolithFlags(this.bits and other.bits.inv())
    fun toggle(other: MonolithFlags) = CombinedMonolithFlags(this.bits xor other.bits)
}