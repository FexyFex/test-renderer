#version 450
#extension GL_ARB_separate_shader_objects : enable

layout (constant_id = 0) const int EXTENT = 2;

struct BoundingBox {
    vec3 min;
    vec3 max;
};

layout (location = 0) in vec3 inFragPos;

layout (set = 0, binding = 0) uniform UBO { float dummy; } cameraBuffer;
layout (set = 0, binding = 1) buffer SBO { int blocks[]; } blockBuffers[16];
layout (set = 0, binding = 4) buffer SBO2 { uint addresses[]; } addressBuffer;
layout (set = 0, binding = 2) uniform texture2D cobbleTex;
layout (set = 0, binding = 3) uniform sampler sampleroni;

layout(push_constant) uniform PushConstants{
    vec4 viewPos;
    ivec4 chunkAddressOffset;
    ivec4 renderDistanceMin;
    ivec4 renderDistanceMax;
};

layout (location = 0) out vec4 outColor;

ivec3 renderDistanceSize = renderDistanceMax.xyz - renderDistanceMin.xyz + 1;

int floorDiv(int x, int y) {
    int r = x / y;
    // if the signs are different and modulo not zero, round down
    if ((x ^ y) < 0 && (r * y != x)) {
        r--;
    }
    return r;
}

int floorMod(int dividend, int modulus) {
    return dividend - (floorDiv(dividend, modulus) * modulus);
}

ivec3 floorMod(ivec3 x, ivec3 y) {
    return ivec3(floorMod(x.x,y.x), floorMod(x.y,y.y), floorMod(x.z,y.z));
}

uint getChunkAddressFromChunkPos(ivec3 chunkPos) {
    ivec3 chunkAddressVector = floorMod((chunkPos + chunkAddressOffset.xyz), renderDistanceSize);
    int chunkAddressIndex = chunkAddressVector.z * renderDistanceSize.z * renderDistanceSize.z + chunkAddressVector.y * renderDistanceSize.y + chunkAddressVector.x;
    return addressBuffer.addresses[chunkAddressIndex];
}

int getBlockAt(ivec3 chunkPos, ivec3 chunkLocalPos) {
    uint chunkAddress = getChunkAddressFromChunkPos(chunkPos);
    if (getChunkAddressFromChunkPos(chunkPos) == -1) return 0;
    uint chunkBufferIndex = chunkAddress & 15u;
    uint chunkOffset = chunkAddress >> 4;
    int index = chunkLocalPos.z * EXTENT * EXTENT + chunkLocalPos.y * EXTENT + chunkLocalPos.x;
    return blockBuffers[chunkBufferIndex].blocks[index + chunkOffset];
}

float intbound(float s, float ds) {
    if (ds < 0.0) {
        return (1.0 - fract(-s)) / -ds;
    } else {
        return (1.0 - fract(s)) / ds;
    }
}

bool posIsInChunkBounds(ivec3 pos) {
    return pos.x >= 0 && pos.x < EXTENT &&
    pos.y >= 0 && pos.y < EXTENT &&
    pos.z >= 0 && pos.z < EXTENT;
}

bool chunkPosIsInRenderBounds(ivec3 chunkPos) {
    //if (getChunkAddressFromChunkPos(chunkPos) == -1) return false;
    return chunkPos.x >= renderDistanceMin.x && chunkPos.x <= renderDistanceMax.x &&
    chunkPos.y >= renderDistanceMin.y && chunkPos.y <= renderDistanceMax.y &&
    chunkPos.z >= renderDistanceMin.z && chunkPos.z <= renderDistanceMax.z;
}

int indexOfMax(vec3 choices) {
    if (choices.x < choices.y && choices.x < choices.z) return 0;
    if (choices.x < choices.y && choices.x >= choices.z) return 2;
    if (choices.x >= choices.y && choices.y < choices.z) return 1;
    //if (choices.x >= choices.y && choices.y >= choices.z) return 2;
    return 2;
}

ivec3 blockPosToChunkPos(ivec3 blockPos) {
    return ivec3(
        blockPos.x / EXTENT - ((blockPos.x < 0 && blockPos.x % EXTENT != 0) ? 1 : 0),
        blockPos.y / EXTENT - ((blockPos.y < 0 && blockPos.y % EXTENT != 0) ? 1 : 0),
        blockPos.z / EXTENT - ((blockPos.z < 0 && blockPos.z % EXTENT != 0) ? 1 : 0)
    );
}

ivec3 blockPosToChunkLocalPos(ivec3 blockPos) {
    int blockPosX = blockPos.x % EXTENT;
    int blockPosY = blockPos.y % EXTENT;
    int blockPosZ = blockPos.z % EXTENT;
    if (blockPosX < 0) blockPosX += EXTENT;
    if (blockPosY < 0) blockPosY += EXTENT;
    if (blockPosZ < 0) blockPosZ += EXTENT;
    return ivec3(blockPosX, blockPosY, blockPosZ);
}

float trueRound(float x) {
    return floor(x + 0.5);
}

void main() {
    vec3 rayStartPoint = viewPos.xyz;

    vec3 direction = inFragPos;
    if (abs(direction.x) < 0.001) direction.x = 0.001;

    ivec3 pos = ivec3(floor(rayStartPoint.x), floor(rayStartPoint.y), floor(rayStartPoint.z));
    ivec3 chunkPos = blockPosToChunkPos(pos);
    ivec3 chunkLocalPos = blockPosToChunkLocalPos(pos);

    ivec3 step = ivec3(sign(direction.x), sign(direction.y), sign(direction.z));
    vec3 tMax = vec3(
        intbound(rayStartPoint.x, direction.x),
        intbound(rayStartPoint.y, direction.y),
        intbound(rayStartPoint.z, direction.z));
    vec3 tDelta = step / direction;

    int faceNormalIndex = -1;
    ivec3 faces = ivec3(-step);
    int block = 0;

    int iters = 0;
    // Based on the fast voxel traversal "Amanatides & Woo" from:
    // https://github.com/cgyurgyik/fast-voxel-traversal-algorithm/blob/master/overview/FastVoxelTraversalOverview.md
    while (chunkPosIsInRenderBounds(chunkPos) && iters < renderDistanceMax.x * 3) {
        if (getChunkAddressFromChunkPos(chunkPos) == -1) {
            iters++;
            ivec3 nextoChunko = chunkPos + step;
            float t = (pos.x + (step.x - 1) / -2 - rayStartPoint.x) / direction.x;
            vec3 hitpos = rayStartPoint + direction * t;
            ivec3 chonkBordar = nextoChunko * EXTENT;
            chonkBordar.x += step.x < 0 ? EXTENT : 0;
            chonkBordar.y += step.y < 0 ? EXTENT : 0;
            chonkBordar.z += step.z < 0 ? EXTENT : 0;
            float tX = abs((chonkBordar.x-hitpos.x)/direction.x);
            float tY = abs((chonkBordar.y-hitpos.y)/direction.y);
            float tZ = abs((chonkBordar.z-hitpos.z)/direction.z);
            vec3 chonkHitpos = vec3(0);
            if (tX < tY && tX < tZ) {
                chonkHitpos = hitpos + direction * tX;
                faceNormalIndex = 0;
            }
            else if (tY < tZ) {
                chonkHitpos = hitpos + direction * tY;
                faceNormalIndex = 1;
            }
            else {
                chonkHitpos = hitpos + direction * tZ;
                faceNormalIndex = 2;
            }
            chonkHitpos += step * 0.0001;
            pos = ivec3(floor(chonkHitpos.x), floor(chonkHitpos.y), floor(chonkHitpos.z));
            chunkPos = blockPosToChunkPos(pos);
            chunkLocalPos = blockPosToChunkLocalPos(pos);
            tMax = vec3(
                intbound(chonkHitpos.x, direction.x),
                intbound(chonkHitpos.y, direction.y),
                intbound(chonkHitpos.z, direction.z));
            continue;
        }
        block = getBlockAt(chunkPos, chunkLocalPos);

        if (block != 0) break;

        int dirIndex = indexOfMax(tMax);
        pos[dirIndex] += step[dirIndex];
        chunkPos = blockPosToChunkPos(pos);
        chunkLocalPos = blockPosToChunkLocalPos(pos);
        tMax[dirIndex] += tDelta[dirIndex];
        faceNormalIndex = dirIndex;
    }

//    outColor = vec4(faceNormalIndex/2.0, 0.0, 0.0, 1.0);
//    return;
    //outColor = vec4(iters/100.0,0.0,0.0, 1.0);
    //uint address = getChunkAddressFromChunkPos(chunkPos);
    //outColor = vec4((1023u & address)/1023.0, (1048575u & address >> 10)/1023.0, (address >> 20)/4095.0,1.0);
    //outColor = address == 0 ? vec4(1.0,.0,.0,1.0) : address == -1 ? vec4(.0,.0,1.0,1.0) : (address >> 28) == 4095 ? vec4(.0,1.0,.0,1.0) : vec4(1.0,1.0,.0,1.0);
    //outColor = vec4(chunkPos * 0.5 + 0.5, 1.0);
    //return;

    if (block == 0) {
        discard;
    } else {
        vec2 texCoords = vec2(0.0);
        if (faceNormalIndex == 0) {
            float t = (pos.x + (step.x - 1) / -2 - rayStartPoint.x) / direction.x;
            vec3 hitpos = rayStartPoint + direction * t;
            if (step.x < 0) texCoords.x = 1 - fract(hitpos.z);
            else texCoords.x = fract(hitpos.z);
            texCoords.y = 1 - fract(hitpos.y);
        }
        else if (faceNormalIndex == 1) {
            float t = (pos.y + (step.y - 1) / -2 - rayStartPoint.y) / direction.y;
            vec3 hitpos = rayStartPoint + direction * t;
            texCoords.x = fract(hitpos.x);
            if (step.y < 0) texCoords.y = fract(hitpos.z);
            else texCoords.y = 1 - fract(hitpos.z);
        }
        else {
            float t = (pos.z + (step.z - 1) / -2 - rayStartPoint.z) / direction.z;
            vec3 hitpos = rayStartPoint + direction * t;
            if (step.z < 0) texCoords.x = fract(hitpos.x);
            else texCoords.x = 1 - fract(hitpos.x);
            texCoords.y = 1 - fract(hitpos.y);
        }
        outColor = texture(sampler2D(cobbleTex, sampleroni), texCoords, 1.0);
        if (faceNormalIndex == 0) outColor *= 0.8;
        else if (faceNormalIndex == 2) outColor *= 0.6;
        else if (faceNormalIndex == 1 && step.y > 0) outColor *= 0.4;
        outColor[3] = 1.0;
        gl_FragDepth = 1.0 - distance(viewPos.xyz, vec3(pos)) / 2000.0;
    }

    //outColor = mix(outColor, vec4(0.0, 0.0, 0.0, 1.0), distance(viewPos.xyz, vec3(pos)) / 32.0);
}
