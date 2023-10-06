package me.fexus.vulkan.component.debug

fun combined(vararg type: Flag): Int {
    return combined(*type.map(Flag::mask).toIntArray())
}

fun combined(vararg int: Int): Int {
    var result = 0
    int.forEach { result = result or it }
    return result
}
