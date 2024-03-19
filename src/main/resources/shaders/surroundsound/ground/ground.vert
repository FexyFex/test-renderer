#version 460
#extension GL_ARB_separate_shader_objects : enable
#extension GL_GOOGLE_include_directive : enable

#include "../defines.glsl"


layout (location = 0) in vec4 inPosition;

layout (set = 0, binding = 0) uniform UBO {
    mat4 view;
    mat4 proj;
} cameraBuffer;
//layout (set = 0, binding = 3) buffer MonolithBuffer { MonolithInfo infos[]; } monolithBuffer;

layout(push_constant) uniform PushConstants { mat4 dum; };

layout (location = 0) out vec3 outColor;


void main() {
    vec4 position = vec4(inPosition.xyz, 1.0);

    gl_Position = cameraBuffer.proj * cameraBuffer.view * position;
    float val = (abs(mod(inPosition.y / 1.5, 1.0))) * 0.8;
    float halved = val * 0.5;
    outColor = vec3(0.2 + halved, 0.2 + val, 0.2 + halved);
}