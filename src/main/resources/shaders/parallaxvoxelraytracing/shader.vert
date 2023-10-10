#version 450
#extension GL_ARB_separate_shader_objects : enable

layout (constant_id = 0) const int EXTENT = 2;

struct BoundingBox {
    vec3 min;
    vec3 max;
};

layout (location = 0) in vec4 inPosition;
layout (location = 1) in vec4 inTexCoords;
layout (location = 2) in vec4 inNormal;

layout (set = 0, binding = 0) uniform UBO {
    mat4 view;
    mat4 proj;
} cameraBuffer;

layout(push_constant) uniform PushConstants{
    mat4 modelMatrix;
    vec4 viewPos;
    int doWireFrame;
};

layout (location = 0) out vec3 outFragPos;
layout (location = 1) out vec3 outNormal;
layout (location = 2) out BoundingBox outBounds;

void main() {
    gl_Position  = cameraBuffer.proj * cameraBuffer.view * modelMatrix * inPosition;
    outFragPos   = vec3(modelMatrix * inPosition);

    outNormal = inNormal.xyz;

    vec3 pos = vec3(modelMatrix[3][0], modelMatrix[3][1], modelMatrix[3][2]);
    outBounds.min = pos + vec3(0.5, 0.5, 1.0 - EXTENT);
    outBounds.max = outBounds.min + EXTENT;
}