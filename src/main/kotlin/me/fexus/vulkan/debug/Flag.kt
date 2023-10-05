package me.fexus.vulkan.debug

interface Flag {
    val mask: Int

    companion object {
        inline fun <reified T> find(flags: Int): Collection<T> where T : Enum<T>, T : Flag {
            return find(T::class.java, flags)
        }

        fun <T> find(clazz: Class<T>, flags: Int): Collection<T> where T : Enum<T>, T : Flag {
            return clazz.enumConstants.filter { it.mask and flags != 0 }
        }
    }
}
