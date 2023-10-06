package me.fexus.vulkan.component.debug.utils

enum class AnsiFormat(val format: String) {
    RESET("\u001B[0m"),
    PHAT("\u001B[1m"),
    UNDERLINE("\u001B[2m");


    override fun toString(): String {
        return format
    }
}