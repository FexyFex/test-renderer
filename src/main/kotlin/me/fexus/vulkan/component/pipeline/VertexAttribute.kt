package me.fexus.vulkan.component.pipeline

data class VertexAttribute(val location: Int, val format: VertexAttributeFormat, val offset: Int, val binding: Int = 0)
