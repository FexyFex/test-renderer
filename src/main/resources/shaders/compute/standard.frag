#version 450
#extension GL_ARB_separate_shader_objects : enable
#extension GL_EXT_nonuniform_qualifier : enable

struct GeneralInfo {
    vec2 cameraPosition;
    vec2 cameraExtent;
    uint tickCounter;
};


layout (location = 0) in vec2 inFragCoords;

layout (set = 0, binding = 0) uniform UBO { GeneralInfo data; } generalInfoBuffer;

layout (push_constant) uniform PushConstants {
    uint initDataBufferIndex;
    uint finalDataBufferIndex;
};

layout (location = 0) out vec4 outColor;


void main() {
    float dist = clamp(1.0 - (distance(inFragCoords, vec2(0.5)) * 3.0), 0.0, 1.0);
    outColor = vec4(0.7, 0.4, 0.4, clamp(pow(dist, 4.0), 0.0, 1.0));
}
