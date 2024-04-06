package me.fexus.examples.coolvoxelrendering.util


inline fun <reified T>Collection<*>.firstInstanceOfOrNull(): T? {
    for (i in this) if (i is T) return i
    return null
}

inline fun <reified T> Collection<*>.containsInstanceOf(): Boolean {
    for (i in this) if (i is T) return true
    return false
}