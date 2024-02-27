#version 450
#extension GL_ARB_separate_shader_objects : enable

layout (location = 0) in vec2 inTexCoords;

layout (set = 0, binding = 0) uniform texture2D textures[8];
layout (set = 0, binding = 1) uniform sampler defaultSampler;

layout(push_constant) uniform PushConstants{
    uint gameImageIndex;
};

layout (location = 0) out vec4 outColor;

void main() {
    vec4 blocksTexture = texture(sampler2D(textures[0], defaultSampler), inTexCoords, 1.0);

    outColor = blocksTexture;
}
