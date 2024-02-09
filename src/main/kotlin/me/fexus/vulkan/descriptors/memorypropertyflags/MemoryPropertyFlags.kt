package me.fexus.vulkan.descriptors.memorypropertyflags


interface MemoryPropertyFlags {
    val vkBits: Int

    infix fun or(other: MemoryPropertyFlags) = this + other
    operator fun plus(other: MemoryPropertyFlags) = CombinedMemoryPropertyFlags(this.vkBits or other.vkBits)

    operator fun contains(property: MemoryPropertyFlags): Boolean {
        return (this.vkBits and property.vkBits) == property.vkBits
    }

    operator fun contains(flags: Int): Boolean {
        return (this.vkBits and flags) == flags
    }

    fun info(): String {
        val properties = enumValues<MemoryPropertyFlag>()

        val containedUsages = mutableListOf<String>()

        properties.forEach {
            if (it in this)
                containedUsages.add(it.name)
        }

        return buildString {
            append("Usages: ")

            containedUsages.forEach {
                append("$it, ")
            }

            removeSuffix(",")
        }
    }
}