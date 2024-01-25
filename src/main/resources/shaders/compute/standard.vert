#version 450
#extension GL_ARB_separate_shader_objects : enable

layout (set = 0, binding = 0) uniform UBO { vec4 frameInfo; } cameraBuffer;

layout (location = 0) in vec4 inPosition;
layout (location = 1) in vec4 inUV;

layout(push_constant) uniform PushConstants{
    vec2 pos;
    vec2 dim;
    float zLayer;
    uint flags;
};

layout (location = 0) out vec2 outFragCoords;


void main() {
    vec2 cameraPos = cameraBuffer.frameInfo.xy;
    vec2 cameraExtent = cameraBuffer.frameInfo.zw;

    vec2 truePos = ((inPosition.xy * dim) + pos + cameraPos) / cameraExtent;

    gl_Position = vec4(truePos, zLayer, 1.0);
    outFragCoords = inUV.xy;
}