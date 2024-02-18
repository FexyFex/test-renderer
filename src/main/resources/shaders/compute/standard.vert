#version 450
#extension GL_ARB_separate_shader_objects : enable

struct GeneralInfo {
    vec2 cameraPosition;
    vec2 cameraExtent;
    uint tickCounter;
};

struct FinalParticleData {
    vec2 position;
    int timeLived;
    uint visualID;
};

layout (constant_id = 0) const int BUFFER_COUNT = 512;
layout (constant_id = 1) const int FRAMES_TOTAL = 5;

layout (set = 0, binding = 0) uniform UBO { GeneralInfo data; } generalInfoBuffer[FRAMES_TOTAL];
layout (set = 0, binding = 1) readonly buffer Buf { FinalParticleData data[]; } buffers[BUFFER_COUNT];

layout (location = 0) in vec4 inPosition;
layout (location = 1) in vec4 inUV;

layout (push_constant) uniform PushConstants {
    uint initDataBufferIndex;
    uint finalDataBufferIndex;
    uint frameIndex;
};

layout (location = 0) out vec2 outFragCoords;


void main() {
    vec2 cameraPosition = generalInfoBuffer[frameIndex].data.cameraPosition;
    vec2 cameraExtent = generalInfoBuffer[frameIndex].data.cameraExtent;

    uint paritcleID = gl_InstanceIndex;
    vec2 particleExtent = vec2(1.0);
    FinalParticleData particle = buffers[finalDataBufferIndex].data[gl_InstanceIndex];

    if (particle.timeLived < 0.0) {
        gl_Position = vec4(0.0 / 0.0);
        outFragCoords = inUV.xy;
        return;
    }

    vec2 truePos = ((inPosition.xy * particleExtent) - particle.position - cameraPosition) / cameraExtent;

    gl_Position = vec4(truePos, 0.1, 1.0);
    outFragCoords = inUV.xy;
}