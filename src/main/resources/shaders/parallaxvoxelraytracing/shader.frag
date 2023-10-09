#version 450
#extension GL_ARB_separate_shader_objects : enable

layout (location = 0) in vec3 inFragPos;
layout (location = 1) in vec2 inTexCoords;
layout (location = 2) in vec3 inTangentViewPos;
layout (location = 3) in vec3 inTangentFragPos;
layout (location = 4) in vec3 inNormal;

layout (set = 0, binding = 1) buffer SBO { float blocks[]; } blockBuffer;

layout(push_constant) uniform PushConstants{
    mat4 modelMatrix;
    vec4 viewPos;
};

layout (location = 0) out vec4 outColor;

void main() {
    vec3 viewDir = (inFragPos.xyz - viewPos.xyz);
    float chunkEnterAngle = 0;

    outColor = vec4(0.5, 0.5, 0.5, 1.0);
}
