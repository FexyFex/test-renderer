#version 450
#extension GL_ARB_separate_shader_objects : enable

struct GeneralInfo {
    vec2 cameraPosition;
    vec2 cameraExtent;
    uint tickCounter;
};

layout (constant_id = 0) const int BUFFER_COUNT = 512;

layout (set = 0, binding = 0) uniform UBO { GeneralInfo data; } generalInfoBuffer;
layout (set = 0, binding = 1) buffer Buf { float dum; } buffers[BUFFER_COUNT];

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
    vec2 cameraPosition = generalInfoBuffer.data.cameraPosition;
    vec2 cameraExtent = generalInfoBuffer.data.cameraExtent;

    vec2 truePos = ((inPosition.xy * dim) + pos + cameraPosition) / cameraExtent;

    gl_Position = vec4(truePos, zLayer, 1.0);
    outFragCoords = inUV.xy;
}