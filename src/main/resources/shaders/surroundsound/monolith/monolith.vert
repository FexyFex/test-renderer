#version 460
#extension GL_ARB_separate_shader_objects : enable
#extension GL_GOOGLE_include_directive : enable

#include "../defines.glsl"


layout (location = 0) in vec4 inPosition;
layout (location = 1) in vec4 inNormal;
layout (location = 2) in vec2 inTexCoords;
layout (location = 3) in uint inVertexGroupID;

layout (set = 0, binding = 0) uniform UBO { WorldInfo info; } cameraBuffer;
layout (set = 0, binding = 3) buffer MonolithBuffer { MonolithInfo infos[]; } monolithBuffer;

layout(push_constant) uniform PushConstants{
    mat4 modelMatrix;
    vec4 lightPos;
    vec4 viewPos;
    float heightScale;
};

layout (location = 0) out vec3 outFragPos;
layout (location = 1) out vec2 outTexCoords;
layout (location = 2) out vec3 outNormal;
layout (location = 3) flat out uint outVertexGroupID;
layout (location = 4) flat out float outTime;
layout (location = 5) flat out uint outMonolithBitFlags;


void main() {
    WorldInfo world = cameraBuffer.info;
    MonolithInfo monolith = monolithBuffer.infos[gl_InstanceIndex];

    gl_Position  = world.proj * world.view * monolith.modelMatrix * inPosition;

    outFragPos   = vec3(modelMatrix * inPosition);
    outTexCoords = inTexCoords.xy;
    outNormal = inNormal.xyz;
    outVertexGroupID = inVertexGroupID;
    outTime = world.time;
    outMonolithBitFlags = monolith.bitFlags;
}