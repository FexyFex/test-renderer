#version 460
#extension GL_ARB_separate_shader_objects : enable

struct WorldInfo {
    mat4 view;
    mat4 proj;
    float time;
};


layout (location = 0) in vec4 inPosition;
layout (location = 1) in vec4 inTexCoords;
layout (location = 2) in vec4 inNormal;

layout (set = 0, binding = 0) uniform UBO { WorldInfo info; } cameraBuffer;

layout(push_constant) uniform PushConstants { mat4 dum; };

layout (location = 0) out vec3 outTexCoords;


void main() {
    WorldInfo world = cameraBuffer.info;
    world.view[3][0] = 0.0;
    world.view[3][1] = 0.0;
    world.view[3][2] = 0.0;
    gl_Position = world.proj * world.view * inPosition;
    outTexCoords = inTexCoords.xyz;
}