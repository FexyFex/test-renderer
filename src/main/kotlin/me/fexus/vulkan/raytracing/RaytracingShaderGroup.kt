package me.fexus.vulkan.raytracing

data class RaytracingShaderGroup(
    val type: RaytracingShaderGroupType,
    val generalShaderIndex: Int,
    val closestHitShaderIndex: Int,
    val anyHitShaderIndex: Int,
    val intersectionShaderIndex: Int
) {
    companion object {
        const val UNUSED = -1
    }
}