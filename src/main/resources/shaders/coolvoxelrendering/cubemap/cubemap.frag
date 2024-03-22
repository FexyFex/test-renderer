#version 460
#extension GL_ARB_separate_shader_objects : enable

layout (location = 0) in vec3 inTexCoords;

layout (set = 0, binding = 1) uniform texture2DArray textureArr[16];
layout (set = 0, binding = 2) uniform sampler samplers[4];

layout(push_constant) uniform PushConstants { uint textureIndex; };

layout (location = 0) out vec4 outColor;


void main() {
    outColor = texture(sampler2DArray(textureArr[textureIndex], samplers[1]), inTexCoords.xyz);
}
