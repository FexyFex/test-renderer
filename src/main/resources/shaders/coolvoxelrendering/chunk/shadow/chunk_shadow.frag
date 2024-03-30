#version 460
#extension GL_ARB_separate_shader_objects : enable

layout (location = 0) in vec3 inFragPos;

layout (set = 0, binding = 1) uniform texture2DArray textures[16];
layout (set = 0, binding = 2) uniform sampler samplers[4];

layout (location = 0) out vec4 outColor;

// Depth is automatically written to the depth attachment
void main() {
    outColor = vec4(0.0);
    outColor.r = inFragPos.z;
}
