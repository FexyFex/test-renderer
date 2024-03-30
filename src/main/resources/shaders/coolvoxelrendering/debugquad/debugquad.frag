#version 460
#extension GL_ARB_separate_shader_objects : enable

layout (location = 0) in vec2 inTexCoords;
layout (location = 1) flat in uint inTextureIndex;

layout (set = 0, binding = 1) uniform texture2DArray textureOnion[16];
layout (set = 0, binding = 1) uniform texture2D textureArray[16];
layout (set = 0, binding = 2) uniform sampler samplers[4];

layout (location = 0) out vec4 outColor;


void main() {
    vec4 color = texture(sampler2D(textureArray[inTextureIndex], samplers[1]), inTexCoords, 1.0);
    outColor.rgb = color.rgb;
    outColor.w = 1.0;
}
