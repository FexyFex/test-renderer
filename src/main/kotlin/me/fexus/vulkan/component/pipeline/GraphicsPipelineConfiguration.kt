package me.fexus.vulkan.component.pipeline

import me.fexus.vulkan.component.pipeline.specializationconstant.SpecializationConstant

data class GraphicsPipelineConfiguration(
    val vertexAttributes: List<VertexAttribute>,

    val pushConstantsLayout: PushConstantsLayout,

    val vertShaderCode: ByteArray,
    val fragShaderCode: ByteArray,

    val specializationConstants: List<SpecializationConstant<*>> = emptyList(),

    val dynamicStates: List<DynamicState> = emptyList(),

    val blendEnable: Boolean = false,
    val primitive: Primitive = Primitive.TRIANGLES,
    val polygonMode: PolygonMode = PolygonMode.FILL,
    val cullMode: CullMode = CullMode.BACKFACE,

    val multisampling: Int = 1,

    val depthTest: Boolean = true,
    val depthWrite: Boolean = true
) {
    val vertexStride: Int = if (vertexAttributes.isEmpty()) 0 else vertexAttributes.last().offset + vertexAttributes.last().format.size
}
