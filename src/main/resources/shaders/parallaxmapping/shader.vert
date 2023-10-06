#version 450
#extension GL_ARB_separate_shader_objects : enable
#extension GL_EXT_nonuniform_qualifier : enable

struct GeneralInfo {
    mat4 view;
    mat4 proj;
    ivec3 cameraChunkPos;
    float time;
    ivec2 screenExtent;
};

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec2 inTexCoords;

layout (set = 0, binding = 0) uniform UBO { GeneralInfo info; } uniformBuffers[];
layout (set = 1, binding = 0) uniform texture2D textures[];
layout (set = 2, binding = 0) uniform sampler samplers[];
layout (set = 3, binding = 0) buffer SBO { float dummy; } storageBuffers[];

layout(push_constant) uniform PushConstants{
    ivec4 position;
    ivec3 chunkPos;
};

uint getIndex(int indexSlot) {
    int indices = position.x;
    int shift = (8 * indexSlot);
    return (indices & (255 << shift)) >> shift;
}

void main() {
    GeneralInfo info = uniformBuffers[getIndex(0)].info;
    vec4 pos = vec4(inPosition + (position.yzw + (chunkPos - info.cameraChunkPos) * 16), 1.0);
    gl_Position = info.proj * info.view * pos;
}