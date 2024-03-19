#version 460
#extension GL_ARB_separate_shader_objects : enable
#extension GL_GOOGLE_include_directive : enable

#include "../../defines.glsl"

const float minHeight = -5.3;
const float maxHeight = 6.3;
const float maxDiameter = 3.0;


layout (location = 0) in vec3 inPosition;
layout (location = 1) in vec2 inTexCoords;
layout (location = 2) in vec3 inNormal;

layout (set = 0, binding = 0) uniform UBO { WorldInfo info; } cameraBuffer;
layout (set = 0, binding = 3) buffer MonolithBuffer { MonolithInfo infos[]; } monolithBuffer[16];

layout(push_constant) uniform PushConstants {
    uint monolithBufferIndex;
};

layout (location = 0) out vec2 outTexCoords;
layout (location = 1) flat out uint outMonolithBitFlags;


void main() {
    WorldInfo world = cameraBuffer.info;
    MonolithInfo monolith = monolithBuffer[monolithBufferIndex].infos[gl_InstanceIndex];

    float time = world.time;
    //float progress = (sin(time) + 1.0) * 0.5;
    float progress = mod(time, 1.0);
    float scale = (0.5 - abs(progress - 0.5)) * maxDiameter;

    vec3 position = (inPosition * scale);
    //position.y += mix(minHeight, maxHeight, progress);
    float dotA = 0.5;
    float theta = acos(dotA) * progress;
    float relativeVec = (maxHeight - (minHeight * dotA));
    position.y += ((minHeight * cos(theta)) + relativeVec * sin(theta));

    gl_Position  = world.proj * world.view * monolith.modelMatrix * vec4(position, 1.0);

    outTexCoords = inTexCoords.xy;
    outMonolithBitFlags = monolith.bitFlags;
}