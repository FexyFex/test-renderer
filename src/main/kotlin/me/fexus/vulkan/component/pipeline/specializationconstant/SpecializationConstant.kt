package me.fexus.vulkan.component.pipeline.specializationconstant

interface SpecializationConstant<T: Number> {
    val id: Int
    val value: T
}