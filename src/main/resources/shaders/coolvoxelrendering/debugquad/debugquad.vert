#version 460
#extension GL_ARB_separate_shader_objects : enable
#extension GL_EXT_nonuniform_qualifier : enable


layout (location = 0) in vec3 inPosition;
layout (location = 1) in vec2 inTexCoords;

layout (set = 0, binding = 0) uniform UBO { float dummy; } cameraBuffer[];
layout (set = 0, binding = 3) buffer ChunkBuffer { float dummy[]; } chunkPosBuf[];

layout (push_constant) uniform PushConstants {
    vec2 pos;
    vec2 extent;
    uint textureIndex;
};

layout (location = 0) out vec2 outTexCoords;
layout (location = 1) flat out uint outTextureIndex;

void main() {
    gl_Position = vec4(inPosition.xy * extent, 0.1, 1.0) + vec4(pos, 0.0, 0.0);

    outTexCoords = inTexCoords - vec2(0.0, 0.0);
    outTexCoords.y = abs(outTexCoords.y - 1.0);
    outTextureIndex = textureIndex;
}