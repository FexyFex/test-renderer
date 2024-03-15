#version 460
#extension GL_ARB_separate_shader_objects : enable
#extension GL_GOOGLE_include_directive : enable

#include "../defines.glsl"


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

    gl_Position  = world.proj * world.view * monolith.modelMatrix * vec4(inPosition, 1.0);

    outTexCoords = inTexCoords.xy;
    outMonolithBitFlags = monolith.bitFlags;
}