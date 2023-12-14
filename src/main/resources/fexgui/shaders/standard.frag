#version 450

layout (set = 1, binding = 0) uniform texture2D textures[64];
layout (set = 0, binding = 1) uniform sampler sampleroni;

layout (location = 0) in vec2 inTexCoords;
layout (location = 1) flat in int inTexIndex;
layout (location = 2) flat in vec4 inBaseColor;

layout (location = 0) out vec4 outColor;


void main() {

    outColor = texture(sampler2D(textures[inTexIndex], sampleroni), inTexCoords.xy, 1.0);
}
