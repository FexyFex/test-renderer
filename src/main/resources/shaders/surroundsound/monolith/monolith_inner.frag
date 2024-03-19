#version 460
#extension GL_ARB_separate_shader_objects : enable

layout (location = 0) in vec4 inFragPos;
layout (location = 1) in vec4 inTexCoords;
layout (location = 2) flat in float inTime;
layout (location = 3) flat in uint inMonolithBitFlags;

layout (set = 0, binding = 1) uniform texture2D textures[16];
layout (set = 0, binding = 2) uniform sampler defaultSampler;

layout (location = 0) out vec4 outColor;



void main() {
    outColor = vec4((sin(inTime) + 1.0) / 2.0, 0.24, 0.4, 1.0);//texture(sampler2D(textures[0], defaultSampler), inTexCoords.xy, 1.0);
}
