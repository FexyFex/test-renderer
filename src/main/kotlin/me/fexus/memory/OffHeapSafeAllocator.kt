package me.fexus.memory

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.Struct
import org.lwjgl.system.StructBuffer
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KTypeParameter


class OffHeapSafeAllocator private constructor(
    private val offHeapBuffersAddresses: MutableList<Long>,
    private val vkStructs: MutableList<Struct>,
    private val vkStructBuffers: MutableList<StructBuffer<*,*>>
) {
    fun allocate(size: Int): ByteBuffer {
        val buf = MemoryUtil.memAlloc(size)
        val bufAdr = MemoryUtil.memAddress(buf)
        offHeapBuffersAddresses.add(bufAdr)
        return buf
    }

    fun allocateInt(count: Int): IntBuffer {
        val buf = MemoryUtil.memAllocInt(count)
        val bufAdr = MemoryUtil.memAddress(buf)
        offHeapBuffersAddresses.add(bufAdr)
        return buf
    }

    fun allocateLong(count: Int): LongBuffer {
        val buf = MemoryUtil.memAllocLong(count)
        val bufAdr = MemoryUtil.memAddress(buf)
        offHeapBuffersAddresses.add(bufAdr)
        return buf
    }

    fun allocateFloat(count: Int): FloatBuffer {
        val buf = MemoryUtil.memAllocFloat(count)
        val bufAdr = MemoryUtil.memAddress(buf)
        offHeapBuffersAddresses.add(bufAdr)
        return buf
    }

    fun allocatePointer(count: Int): PointerBuffer {
        val buf = MemoryUtil.memAllocPointer(count)
        val bufAdr = MemoryUtil.memAddress(buf)
        offHeapBuffersAddresses.add(bufAdr)
        return buf
    }

    fun allocateString(string: String): ByteBuffer {
        val buf = MemoryUtil.memUTF8(string)
        val bufAdr = MemoryUtil.memAddress(buf)
        offHeapBuffersAddresses.add(bufAdr)
        return buf
    }

    fun <S: Struct> calloc(callocFun: () -> S, configure: S.() -> Unit = {}): S {
        val struct: S = callocFun()
        struct.configure()
        vkStructs.add(struct)
        return struct
    }

    fun <B: StructBuffer<*,*>> calloc(callocFun: (Int) -> B, count: Int): B {
        val structBuffer = callocFun(count)
        vkStructBuffers.add(structBuffer)
        return structBuffer
    }

    companion object {
        @OptIn(ExperimentalContracts::class)
        fun <R> runMemorySafe(block: OffHeapSafeAllocator.() -> R): R {
            contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

            val offHeapBufferAddresses = mutableListOf<Long>()
            val vkStructs = mutableListOf<Struct>()
            val vkStructBuffers = mutableListOf<StructBuffer<*,*>>()
            val context = OffHeapSafeAllocator(offHeapBufferAddresses, vkStructs, vkStructBuffers)
            val ret = context.block()
            offHeapBufferAddresses.forEach { MemoryUtil.nmemFree(it) }
            vkStructs.forEach { it.free() }
            vkStructBuffers.forEach { it.free() }
            return ret
        }
    }
}
