package me.fexus.vulkan.descriptors.memoryproperties


interface MemoryProperties {
    val vkBits: Int

    infix fun or(other: MemoryProperties) = this + other
    operator fun plus(other: MemoryProperties) = CombinedMemoryProperties(this.vkBits or other.vkBits)

    operator fun contains(property: MemoryProperties): Boolean {
        return (this.vkBits and property.vkBits) == property.vkBits
    }

    operator fun contains(flags: Int): Boolean {
        return (this.vkBits and flags) == flags
    }

    fun info(): String {
        val properties = enumValues<MemoryProperty>()

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