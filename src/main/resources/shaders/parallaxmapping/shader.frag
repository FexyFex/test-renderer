#version 450
#extension GL_ARB_separate_shader_objects : enable

const int DIFFUSE_MAP_INDEX = 0;
const int NORMAL_MAP_INDEX = 1;
const int DEPTH_MAP_INDEX= 2;

layout (location = 0) in vec3 inFragPos;
layout (location = 1) in vec2 inTexCoords;
layout (location = 2) in vec3 inTangentLightPos;
layout (location = 3) in vec3 inTangentViewPos;
layout (location = 4) in vec3 inTangentFragPos;

layout (set = 0, binding = 1) buffer SBO { int blocks[]; } blockBuffer;
layout (set = 0, binding = 2) uniform sampler defaultSampler;
layout (set = 0, binding = 3) uniform texture2D maps[3];

layout(push_constant) uniform PushConstants{
    mat4 modelMatrix;
    vec4 lightPos;
    vec4 viewPos;
    float heightScale;
};

layout (location = 0) out vec4 outColor;

void main() {
    outColor = vec4(0.5,0.5,0.5,1.0);
}
