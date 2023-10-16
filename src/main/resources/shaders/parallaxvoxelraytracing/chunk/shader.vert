#version 450
#extension GL_ARB_separate_shader_objects : enable
#extension GL_EXT_nonuniform_qualifier : enable

layout (location = 0) out vec2 outTexCoords;
layout (location = 1) out vec3 outRayDirection;

layout(push_constant) uniform PushConstants{
    vec4 viewPos;
    vec4 viewDirection;
    ivec4 chunkAddressOffset;
    ivec4 renderDistanceMin;
    ivec4 renderDistanceMax;
    vec4 upDirection;
    vec4 rightDirection;
    float fov;
    float aspectRatio;
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
    int vID = gl_VertexIndex;

    vec2 n = vec2(((vID << 1) & 2), vID & 2);

    bool is4 = (vID == 4);
    bool is5 = (vID == 5);

    n.x = (int(is4) * 0) + (int(!is4) * n.x);
    n.y = (int(is4) * 2) + (int(!is4) * n.y);
    n.x = (int(is5) * 2) + (int(!is5) * n.x);
    n.y = (int(is5) * 0) + (int(!is5) * n.y);

    outTexCoords = n / 2.0;
    gl_Position = vec4(n - 1.0, 0.5, 1.0);

    if (outTexCoords == vec2(0,0)) {
        outRayDirection = slerp(viewDirection.xyz, rightDirection.xyz * gl_Position.x, fov/180);
        outRayDirection = slerp(outRayDirection, upDirection.xyz * gl_Position.y, fov * aspectRatio / 180);
    }
}