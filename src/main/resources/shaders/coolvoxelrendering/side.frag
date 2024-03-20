#version 460
#extension GL_ARB_separate_shader_objects : enable

layout (location = 0) in vec2 inTexCoords;

layout (set = 0, binding = 1) uniform texture2D textures[16];
layout (set = 0, binding = 2) uniform sampler defaultSampler;

layout (location = 0) out vec4 outColor;


void main() {
    outColor = vec4(0.5, 0.8, 0.38, 1.0);//texture(sampler2D(textures[0], defaultSampler), inTexCoords, 1.0);
}
