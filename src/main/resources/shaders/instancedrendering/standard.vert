#version 450
#extension GL_ARB_separate_shader_objects : enable

struct Camera{
    mat4 view;
    mat4 proj;
};


layout (set = 0, binding = 0) uniform UBO { Camera camera; } cameraBuffer;
layout (set = 0, binding = 1) buffer VoxelBuffer { vec4 voxelColors[]; } voxelBuffer;

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


void main() {
    vec4 voxelColor = voxelBuffer.voxelColors[gl_InstanceIndex];

    if (voxelColor.w <= 0.0) {
        gl_Position = vec4(0.0/0.0);
        outColor = vec4(0.0);
        return;
    }

    vec4 voxelPos = vec4(inPosition.xyz + indexToPosition(gl_InstanceIndex), 1.0);
    gl_Position = cameraBuffer.camera.proj * cameraBuffer.camera.view * voxelPos;
    outColor = voxelColor;
}