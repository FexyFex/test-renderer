#version 450
#extension GL_ARB_separate_shader_objects : enable

struct BoundingBox {
    vec3 min;
    vec3 max;
};

layout (location = 0) in vec3 inFragPos;
layout (location = 1) in vec2 inTexCoords;
layout (location = 2) in vec3 inTangentViewPos;
layout (location = 3) in vec3 inTangentFragPos;
layout (location = 4) in vec3 inNormal;
layout (location = 5) in BoundingBox inBounds;

layout (set = 0, binding = 1) buffer SBO { float blocks[]; } blockBuffer;

layout(push_constant) uniform PushConstants{
    mat4 modelMatrix;
    vec4 viewPos;
};

layout (location = 0) out vec4 outColor;


int indexOfMax(vec3 choices) {
    if (choices.x >= choices.y && choices.x >= choices.z) return 0;
    if (choices.y >= choices.x && choices.y >= choices.z) return 1;
    if (choices.z >= choices.z && choices.z >= choices.y) return 2;
}

void main() {
    // Based on the fast voxel traversal "Amanatides & Woo" from:
    // https://github.com/cgyurgyik/fast-voxel-traversal-algorithm/blob/master/overview/FastVoxelTraversalOverview.md

    vec3 direction = (inFragPos.xyz - viewPos.xyz);
    vec3 origin = inFragPos - inBounds.min; // starting from the first intersection (hopefully?)

    float stepX = sign(direction.x);
    float stepY = sign(direction.y);
    float stepZ = sign(direction.z);

    vec3 tMax = vec3(0.0);
    for (i = 0; i < 3; i++) {
        int curr = max(1, ceil(origin[i] - inBounds.min[i]));
        tMax[i] = (inBounds.min[i] + curr - origin[i]) / direction[i];
    }

    // TODO: make it work in all directions (pos to neg and vice versa)

    while (true) {
        int dirIndex = indexOfMax(tMax);

        int curr = max(1, ceil(origin[dirIndex] - inBounds.min[dirIndex]));
        tMax[dirIndex] = (inBounds.min[dirIndex] + curr - origin[dirIndex]) / direction[dirIndex];

        vec3 nextVoxelCoords = vec3(0.0);
    }

    outColor = vec4(0.5, 0.5, 0.5, 1.0);
}
