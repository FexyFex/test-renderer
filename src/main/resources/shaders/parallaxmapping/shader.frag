#version 450
#extension GL_ARB_separate_shader_objects : enable
#extension GL_EXT_nonuniform_qualifier : enable

layout (set = 0, binding = 0) uniform UBO { float dummy; } uniformBuffers[];
layout (set = 0, binding = 0) uniform MatrixUBO { mat4 transform; } matrixubo[];
layout (set = 1, binding = 0) uniform texture2D textures[];
layout (set = 2, binding = 0) uniform sampler samplers[];
layout (set = 3, binding = 0) buffer SBO { float dummy; } storageBuffers[];

layout (location = 0) out vec4 outColor;

void main() {
    outColor = vec4(0.0,0.0,0.0,1.0);
}
