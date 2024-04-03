#version 460
#extension GL_ARB_separate_shader_objects : enable

layout (location = 0) in vec3 inFragPos;
layout (location = 1) in vec2 inTexCoords;
layout (location = 2) flat in uint textureIndex;
layout (location = 3) flat in float inSideLight;

layout (set = 0, binding = 1) uniform texture2DArray texturesOnion[16];
layout (set = 0, binding = 2) uniform sampler samplers[4];

layout (location = 0) out vec4 outColor;

// Depth is automatically written to the depth attachment
void main() {
    outColor = vec4(1.0);
    //vec4 worldColor = texture(sampler2DArray(texturesOnion[0], samplers[0]), vec3(inTexCoords, float(textureIndex)));
    //outColor = worldColor;
    //outColor.xyz *= inSideLight;
    //outColor = worldColor;
}
