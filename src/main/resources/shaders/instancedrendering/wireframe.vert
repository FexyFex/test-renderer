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
    mat4 modelMatrix;
};

layout (location = 0) out vec4 outColor;

void main() {
    vec4 position = vec4(inPosition.xyz * 0.5, 1.0);
    gl_Position = cameraBuffer.camera.proj * cameraBuffer.camera.view * modelMatrix * position;
    outColor = vec4(0.3, 1.0, 0.2, 1.0);
}