#version 450
#extension GL_ARB_separate_shader_objects : enable

const int CHILD_POINTER_SIZE_INTS = 2;

struct Camera{
    mat4 view;
    mat4 proj;
};


layout (set = 0, binding = 0) uniform UBO { Camera camera; } cameraBuffer;
layout (set = 0, binding = 1) buffer OctreeBuffer { int octreedata[]; } octreeBuffer;

layout (location = 0) in vec4 inPosition;

layout(push_constant) uniform PushConstants{
    int extent;
};

layout (location = 0) out vec4 outColor;


vec3 indexToPosition(int index) {
    int fx = index % extent;
    int fy = (index - fx) % (extent * extent);
    int fz = (index - fx - fy);
    return vec3(fx, fy / extent, fz / (extent * extent));
}

int getOctantIndexOfGlobalPositionInMipLevel(vec3 globalPos, int mipLevel) {
    int mipExtent = extent >> mipLevel;
    vec3 rel = floor(globalPos / mipExtent);
    vec3 mid = (rel * mipExtent) + (mipExtent / 2.0);
    int x = int(globalPos.x >= mid.x);
    int y = int(globalPos.y >= mid.y);
    int z = int(globalPos.z >= mid.z);
    return x | (y << 1) | (z << 2);
}

void main() {
    vec3 positionInChunk = indexToPosition(gl_InstanceIndex);
    int mipLevelCount = int(log2(float(extent)));

    int targetBlockExists = 0;
    int offset = 0;

    for (int mipLevel = 0; mipLevel < mipLevelCount; mipLevel++) {
        int childCount = octreeBuffer.octreedata[offset];
        int targetOctantIndex = getOctantIndexOfGlobalPositionInMipLevel(positionInChunk, mipLevel);
        int targetChildOffset = -1;
        for (int cIndex = 0; cIndex < childCount; cIndex++) {
            int childOctantOffset = offset + 1 + (cIndex * CHILD_POINTER_SIZE_INTS);
            int currentOctantIndex = octreeBuffer.octreedata[childOctantOffset];
            if (currentOctantIndex == targetOctantIndex) {
                targetChildOffset = octreeBuffer.octreedata[childOctantOffset + 1];
                break;
            }
        }
        if (targetChildOffset < 0) break;
        if (mipLevel == mipLevelCount - 1) {
            targetBlockExists = 1;
        }
        offset = targetChildOffset >> 2; // divide by 4 since the CPU gives us byte offset, not int offsets
    }

    if (targetBlockExists == 0) {
        gl_Position = vec4(0.0/0.0);
        outColor = vec4(0.0);
        return;
    }

    vec4 voxelPos = vec4(inPosition.xyz + positionInChunk, 1.0);
    gl_Position = cameraBuffer.camera.proj * cameraBuffer.camera.view * voxelPos;
    outColor = vec4(1.0, 0.5, 0.5, 1.0);
}