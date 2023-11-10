#version 450
#extension GL_ARB_separate_shader_objects : enable

struct Camera{
    mat4 view;
    mat4 proj;
};

struct VoxelData {
    vec4 position;
    vec4 color;
};


layout (set = 0, binding = 0) uniform UBO { Camera camera; } cameraBuffer;
layout (set = 0, binding = 1) buffer VoxelBuffer { VoxelData voxels[]; } voxelBuffer;

layout (location = 0) in vec4 inPosition;

layout(push_constant) uniform PushConstants{
    float dum;
};

layout (location = 0) out vec4 outColor;

void main() {
    VoxelData voxel = voxelBuffer.voxels[gl_InstanceIndex];

    if (voxel.color.w <= 0.0) {
        gl_Position = vec4(0/0);
        return;
    }

    vec4 voxelPos = vec4(inPosition.xyz + voxel.position.xyz, 1.0);
    gl_Position = cameraBuffer.camera.proj * cameraBuffer.camera.view * voxelPos;
    outColor = voxel.color;
}