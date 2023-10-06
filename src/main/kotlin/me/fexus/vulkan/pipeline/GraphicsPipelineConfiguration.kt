package me.fexus.vulkan.pipeline

data class GraphicsPipelineConfiguration(
    val vertexAttributes: List<VertexAttribute>,

    val pushConstantsLayout: PushConstantsLayout,

    val vertShaderCode: ByteArray,
    val fragShaderCode: ByteArray,

    val blendEnable: Boolean = false,
    val primitive: Primitive = Primitive.TRIANGLES,
    val polygonMode: PolygonMode = PolygonMode.FILL,
    val cullMode: CullMode = CullMode.BACKFACE,

    val multisampling: Int = 1,

    val depthTest: Boolean = true,
    val depthWrite: Boolean = true
) {
    val vertexStride: Int = vertexAttributes.sumOf { it.format.size }
}
