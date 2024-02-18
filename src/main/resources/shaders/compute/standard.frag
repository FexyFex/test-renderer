#version 450
#extension GL_ARB_separate_shader_objects : enable
#extension GL_EXT_nonuniform_qualifier : enable

struct GeneralInfo {
    vec2 cameraPosition;
    vec2 cameraExtent;
    uint tickCounter;
};


layout (constant_id = 0) const int BUFFER_COUNT = 512;
layout (constant_id = 1) const int FRAMES_TOTAL = 5;

layout (set = 0, binding = 0) uniform UBO { GeneralInfo data; } generalInfoBuffer[FRAMES_TOTAL];

layout (location = 0) in vec2 inFragCoords;

layout (push_constant) uniform PushConstants {
    uint initDataBufferIndex;
    uint finalDataBufferIndex;
    uint frameIndex;
};

layout (location = 0) out vec4 outColor;


void main() {
    float dist = clamp(1.0 - (distance(inFragCoords, vec2(0.5)) * 3.0), 0.0, 1.0);
    outColor = vec4(0.7, 0.1, 0.4, clamp(pow(dist, 4.0), 0.0, 1.0));
}
