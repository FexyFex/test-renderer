package me.fexus.fexgui.util

import me.fexus.math.vec.IVec2
import me.fexus.math.vec.IVec3
import java.nio.ByteBuffer


fun ByteBuffer.put(offset: Int, ivec3: IVec3) = ivec3.intoByteBuffer(this, offset)
fun ByteBuffer.put(offset: Int, ivec2: IVec2) = ivec2.toByteBuffer(this, offset)