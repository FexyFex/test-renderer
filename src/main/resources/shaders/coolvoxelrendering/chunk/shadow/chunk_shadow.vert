#version 460
#extension GL_ARB_separate_shader_objects : enable
#extension GL_EXT_nonuniform_qualifier : enable


struct WorldInfo {
    mat4 view;
    mat4 proj;
    float time;
};

struct SideInfo {
    uvec3 position;
    uvec2 scaling;
    uint dirIndex;
    uint textureIndex;
};

const vec3 dirs[6] = vec3[6](
        vec3(1.0, 0.0, 0.0),
        vec3(0.0, 1.0, 0.0),
        vec3(0.0, 0.0, 1.0),
        vec3(-1.0, 0.0, 0.0),
        vec3(0.0, -1.0, 0.0),
        vec3(0.0, 0.0, -1.0)
);


layout (location = 0) in vec3 inPosition;
layout (location = 1) in vec2 inTexCoords;

layout (set = 0, binding = 0) uniform UBO { WorldInfo info; } cameraBuffer[];
layout (set = 0, binding = 3) buffer ChunkBuffer { ivec4 chunkPos[]; } chunkPosBuf[];
layout (set = 0, binding = 3) buffer PositionBuffer { uint positions[]; } sidePosBuffer[];

layout (push_constant) uniform PushConstants {
    uint cameraBufferIndex;
    uint objInfoBufferIndex;
    uint commandBufferIndex;
    uint chunkPosBufferIndex;
    uint sidePosBufferIndex;
    uint commandBufferShadowIndex;
};

layout (location = 0) out vec3 outFragPos;
layout (location = 1) out vec2 outTexCoords;
layout (location = 2) flat out uint outTextureIndex;
layout (location = 3) flat out float outSideLight;


SideInfo unpack(uint packed) {
    SideInfo side;
    side.position.x = packed & 31;
    side.position.y = (packed >> 5) & 31;
    side.position.z = (packed >> 10) & 31;
    side.scaling.x = (packed >> 15) & 31;
    side.scaling.y = (packed >> 20) & 31;
    side.dirIndex = (packed >> 25) & 7;
    side.textureIndex = (packed >> 28) & 15;
    return side;
}

void main() {
    ivec4 chunkPos = chunkPosBuf[chunkPosBufferIndex].chunkPos[gl_DrawID];

    WorldInfo world = cameraBuffer[cameraBufferIndex].info;
    uint packedInt = sidePosBuffer[sidePosBufferIndex].positions[gl_InstanceIndex];

    SideInfo side = unpack(packedInt);
    vec3 normal = dirs[side.dirIndex];

    vec3 rotatedPos = inPosition;
    vec3 scaling = vec3(1.0);
    if (normal.x != 0.0) {
        rotatedPos.z = rotatedPos.x;
        rotatedPos.x = 0.0;
        scaling.z = side.scaling.x;
        scaling.y = side.scaling.y;
        outSideLight = 1.5;
        if (normal.x > 0.0) {
            rotatedPos.z = 1.0 - rotatedPos.z;
        }
    } else if (normal.y != 0.0) {
        rotatedPos.z = rotatedPos.y;
        rotatedPos.y = 0.0;
        scaling.z = side.scaling.y;
        scaling.x = side.scaling.x;
        outSideLight = 0.33;
        if (normal.y > 0.0) {
            outSideLight = 1.85;
            rotatedPos.z = rotatedPos.z;
            rotatedPos.x = 1.0 - rotatedPos.x;
        }
    } else {
        scaling.x = side.scaling.x;
        scaling.y = side.scaling.y;
        outSideLight = 0.5;
        if (normal.z < 0.0) {
            rotatedPos.y = rotatedPos.y;
            rotatedPos.x = 1.0 - rotatedPos.x;
        }
    }

    vec3 position = (rotatedPos * scaling) + side.position + (chunkPos.xyz);

    gl_Position  = world.proj * world.view * vec4(position, 1.0);

    outFragPos = position;
    outTexCoords = inTexCoords;
    outTextureIndex = side.textureIndex;
}