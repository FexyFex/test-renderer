#define HIT_KIND_AABB 0
#define HIT_KIND_SPHERE 1

#define NODE_TYPE_PARENT 0
#define NODE_TYPE_LEAF 1

struct AABB {
    vec4 min;
    vec4 max;
};

struct VoxelData {
    bool isVoid;
    uint textureIndex;
};


VoxelData voxelDataFromInt(uint voxelDataInt) {
    VoxelData data;
    data.isVoid = (voxelDataInt >> 31) == 1;
    data.textureIndex = voxelDataInt & 2147483647;
    return data;
}



