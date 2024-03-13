#version 460
#extension GL_ARB_separate_shader_objects : enable

layout (location = 0) in vec3 inColor;

layout (set = 0, binding = 1) uniform texture2D textures[16];
layout (set = 0, binding = 2) uniform sampler defaultSampler;

layout (location = 0) out vec4 outColor;


void main() {
    outColor = vec4(inColor, 1.0);
}
