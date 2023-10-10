#version 450
#extension GL_ARB_separate_shader_objects : enable

struct BoundingBox {
    vec3 min;
    vec3 max;
};

layout (location = 0) in vec4 inPosition;
layout (location = 1) in vec4 inNormal;
layout (location = 2) in vec4 inTexCoords;
layout (location = 3) in vec4 inTangent;
layout (location = 4) in vec4 inBitangent;

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
layout (location = 1) out vec2 outTexCoords;
layout (location = 2) out vec3 outTangentViewPos;
layout (location = 3) out vec3 outTangentFragPos;
layout (location = 4) out vec3 outNormal;
layout (location = 5) out BoundingBox outBounds;

void main() {
    gl_Position  = cameraBuffer.proj * cameraBuffer.view * modelMatrix * inPosition;
    outFragPos   = vec3(modelMatrix * inPosition);
    outTexCoords = inTexCoords.xy;

    vec3 t = normalize(mat3(modelMatrix) * inTangent.xyz);
    vec3 b = normalize(mat3(modelMatrix) * inBitangent.xyz);
    vec3 n = normalize(mat3(modelMatrix) * inNormal.xyz);
    mat3 tbn = transpose(mat3(t, b, n));

    outTangentViewPos  = tbn * viewPos.xyz;
    outTangentFragPos  = tbn * outFragPos;

    outNormal = inNormal.xyz;

    outBounds.min = vec3(-0.5,-0.5,0.0);
    outBounds.max = vec3(7.5,7.5,0.0);
}