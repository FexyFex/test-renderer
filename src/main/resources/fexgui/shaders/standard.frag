#version 450

layout (set = 0, binding = 1) uniform texture2D textures[64];
layout (set = 0, binding = 2) uniform sampler sampleroni;

layout (location = 0) in vec2 inTexCoords;
layout (location = 1) flat in int inTexIndex;

layout (location = 0) out vec4 outColor;


void main() {
    outColor = texture(sampler2D(textures[inTexIndex], sampleroni), inTexCoords.xy, 1.0);
}
