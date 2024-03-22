#version 460
#extension GL_ARB_separate_shader_objects : enable

layout (location = 0) in vec2 inTexCoords;
layout (location = 1) flat in uint textureIndex;


layout (set = 0, binding = 1) uniform texture2DArray textures[16];
layout (set = 0, binding = 2) uniform sampler samplers[4];

layout (location = 0) out vec4 outColor;


void main() {
    //outColor = vec4(inTexCoords.x, 0.0, inTexCoords.y, 1.0);
    outColor = texture(sampler2DArray(textures[0], samplers[0]), vec3(inTexCoords, float(textureIndex)));
}
