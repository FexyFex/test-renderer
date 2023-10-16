#version 450
#extension GL_ARB_separate_shader_objects : enable
#extension GL_EXT_nonuniform_qualifier : enable

layout (set = 0, binding = 0) uniform UBO {
    mat4 view;
    mat4 proj;
} cameraBuffer;

layout (location = 0) in vec4 inPosition;
layout (location = 1) in vec4 inTexCoords;
layout (location = 2) in vec4 inNormal;

layout (location = 0) out vec3 outFragPos;

layout(push_constant) uniform PushConstants{
    vec4 viewPos;
    ivec4 chunkAddressOffset;
    ivec4 renderDistanceMin;
    ivec4 renderDistanceMax;
};

//source: https://stackoverflow.com/questions/67919193/how-does-unity-implements-vector3-slerp-exactly
vec3 slerp(vec3 start, vec3 end, float ratio)
{
    // Dot product - the cosine of the angle between 2 vectors.
    float dot = dot(start, end);

    // Clamp it to be in the range of Acos()
    // This may be unnecessary, but floating point
    // precision can be a fickle mistress.
    clamp(dot, -1.0f, 1.0f);

    // Acos(dot) returns the angle between start and end,
    // And multiplying that by percent returns the angle between
    // start and the final result.
    float theta = acos(dot) * ratio;
    vec3 relativeVec = end - start * dot;
    normalize(relativeVec);

    // Orthonormal basis
    // The final result.
    return ((start*cos(theta)) + (relativeVec * sin(theta)));
}

void main() {
    mat4 view = cameraBuffer.view;
    view[3][0] = 0.0;
    view[3][1] = 0.0;
    view[3][2] = 0.0;
    gl_Position = cameraBuffer.proj * cameraBuffer.view * inPosition;
    outFragPos = inPosition.xyz;
}