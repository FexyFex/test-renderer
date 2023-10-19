#version 450
#extension GL_ARB_separate_shader_objects : enable
#extension GL_EXT_nonuniform_qualifier : enable

layout (set = 0, binding = 0) uniform UBO {
    mat4 view;
    mat4 proj;
} cameraBuffer;

layout (location = 0) in vec4 inPosition;
layout (location = 1) in vec4 inTexCoords;
layout (location = 2) in vec4 inNormal;

layout (location = 0) out vec3 outFragPos;

layout(push_constant) uniform PushConstants{
    vec4 viewPos;
    ivec4 chunkAddressOffset;
    ivec4 renderDistanceMin;
    ivec4 renderDistanceMax;
};

void main() {
    mat4 view = cameraBuffer.view;
    view[3][0] = 0.0;
    view[3][1] = 0.0;
    view[3][2] = 0.0;
    gl_Position = cameraBuffer.proj * view * inPosition;
    outFragPos = inPosition.xyz;
}