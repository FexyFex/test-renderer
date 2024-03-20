#version 460
#extension GL_ARB_separate_shader_objects : enable


struct WorldInfo {
    mat4 view;
    mat4 proj;
    float time;
};

const vec3 dirs[6] = vec3[6](
        vec3(1.0, 0.0, 0.0),
        vec3(0.0, 1.0, 0.0),
        vec3(0.0, 0.0, 1.0),
        vec3(-1.0, 0.0, 0.0),
        vec3(0.0, -1.0, 0.0),
        vec3(0.0, 0.0, -1.0)
);


layout (location = 0) in vec4 inPosition;

layout (set = 0, binding = 0) uniform UBO { WorldInfo info; } cameraBuffer;
layout (set = 0, binding = 3) buffer PositionBuffer { uint positions[]; } positionBuffer[16];

layout (push_constant) uniform PushConstants {
    uint positionsBufferIndex;
};

layout (location = 0) out vec2 outTexCoords;


void main() {
    WorldInfo world = cameraBuffer.info;
    uint packet = positionBuffer[positionsBufferIndex].positions[gl_InstanceIndex];

    vec3 position = inPosition.xyz;

    gl_Position  = world.proj * world.view * vec4(position, 1.0);

    outTexCoords = vec2(0.0);
}