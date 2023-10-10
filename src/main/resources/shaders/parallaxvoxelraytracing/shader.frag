#version 450
#extension GL_ARB_separate_shader_objects : enable

layout (constant_id = 0) const int EXTENT = 2;

struct BoundingBox {
    vec3 min;
    vec3 max;
};

layout (location = 0) in vec3 inFragPos;
layout (location = 1) in vec3 inNormal;
layout (location = 2) in BoundingBox inBounds;

layout (set = 0, binding = 1) buffer SBO { int blocks[]; } blockBuffer;

layout(push_constant) uniform PushConstants{
    mat4 modelMatrix;
    vec4 viewPos;
    int doWireFrame;
};

layout (location = 0) out vec4 outColor;


int getBlockAt(ivec3 pos) {
    int index = pos.z * EXTENT * EXTENT + pos.y * EXTENT + pos.x;
    return blockBuffer.blocks[index];
}

float intbound(float s, float ds) {
    if (ds < 0.0) {
        return (1.0 - fract(-s)) / -ds;
    } else {
        return (1.0 - fract(s)) / ds;
    }
}

bool posIsInBounds(ivec3 pos) {
    return pos.x >= 0 && pos.x < EXTENT &&
            pos.y >= 0 && pos.y < EXTENT &&
            pos.z >= 0 && pos.z < EXTENT;
}

int indexOfMax(vec3 choices) {
    if (choices.x < choices.y && choices.x < choices.z) return 0;
    if (choices.x < choices.y && choices.x >= choices.z) return 2;
    if (choices.x >= choices.y && choices.y < choices.z) return 1;
    if (choices.x >= choices.y && choices.y >= choices.z) return 2;
}

vec3 transformPointToLocalCoords(vec3 point) {
    vec3 boundsSize = (inBounds.max - inBounds.min);
    vec3 progress = (point.xyz - inBounds.min) / boundsSize;
    return vec3(
    mix(0.0, boundsSize.x, progress.x),
    mix(0.0, boundsSize.y, progress.y),
    mix(0.0, boundsSize.z, progress.z)
    );
}

void main() {
    // Based on the fast voxel traversal "Amanatides & Woo" from:
    // https://github.com/cgyurgyik/fast-voxel-traversal-algorithm/blob/master/overview/FastVoxelTraversalOverview.md

    if (doWireFrame == 1) {
        outColor = vec4(0.6, 1.0, 0.1, 1.0);
        return;
    }

    // TODO: New logic! Since we will be doing frontface culling, we must calculate the entryPoint by
    // TODO: finding the intersection between the inBounds box and the ray that is cast from the cam to the inFragPos
    // TODO: We must also consider the scenario in which the player stands within the cube. The entrypoint would
    // TODO: be inside of the voxel grid in that case...
    vec3 exitPoint = transformPointToLocalCoords(inFragPos);

    vec3 direction = inFragPos.xyz - viewPos.xyz;

    vec3 entryPoint;
    vec3 localViewPos = transformPointToLocalCoords(viewPos.xyz);
    if (posIsInBounds(ivec3(localViewPos))) {
        entryPoint = localViewPos;
    }
    else {
        float entryX = direction.x < 0 ? EXTENT : 0;
        float tX = (entryX-exitPoint.x)/direction.x;
        vec3 entryPointX = tX*direction + exitPoint;
        float lenX = length(entryPointX - exitPoint);

        float entryY = direction.y < 0 ? EXTENT : 0;
        float tY = (entryY-exitPoint.y)/direction.y;
        vec3 entryPointY = tY*direction + exitPoint;
        float lenY = length(entryPointY - exitPoint);

        float entryZ = direction.z < 0 ? EXTENT : 0;
        float tZ = (entryZ-exitPoint.z)/direction.z;
        vec3 entryPointZ = tZ*direction + exitPoint;
        float lenZ = length(entryPointZ - exitPoint);

        if (lenX < lenY && lenX < lenZ) entryPoint = entryPointX;
        else if (lenY < lenZ) exitPoint = entryPointY;
        else entryPoint = entryPointZ;
    }

    ivec3 pos = ivec3(round(entryPoint.x), round(entryPoint.y), round(entryPoint.z));

    ivec3 step = ivec3(sign(direction.x), sign(direction.y), sign(direction.z));
    vec3 tMax = vec3(
        intbound(entryPoint.x - 0.5, direction.x),
        intbound(entryPoint.y - 0.5, direction.y),
        intbound(entryPoint.z - 0.000001, direction.z));
    vec3 tDelta = step / direction;

    int faceNormalIndex = -1;
    ivec3 faces = ivec3(-step);
    int block = 0;
    bool wasIn = false;

    while (posIsInBounds(pos)) {
        wasIn = true;
        block = getBlockAt(pos);

        if (block != 0) break;

        int dirIndex = indexOfMax(tMax);
        pos[dirIndex] += step[dirIndex];
        tMax[dirIndex] += tDelta[dirIndex];
        faceNormalIndex = dirIndex;
    }

    if (block == 0) {
        if (!wasIn) outColor = vec4(entryPoint.x/EXTENT, entryPoint.y/EXTENT, entryPoint.z/EXTENT, 1.0);
        else discard;
    } else {
        if (faceNormalIndex == 0) outColor = vec4(0.9, 0.5, 0.5, 1.0);
        else if (faceNormalIndex == 1) outColor = vec4(0.5, 0.9, 0.5, 1.0);
        else  outColor = vec4(0.5, 0.5, 0.9, 1.0);
    }
}
