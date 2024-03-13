#define MONOLITH_NEAR_TRIGGER_BIT 1
#define MONOLITH_TRIGGERED_BIT 2
#define MONOLITH_HUMMING_BIT 4

struct WorldInfo {
    mat4 view;
    mat4 proj;
    float time;
};

struct MonolithInfo {
    mat4 modelMatrix;
    uint bitFlags;

    // I have no idea know padding works lol
    uint dum;
    ivec2 dum2;
    ivec4 dum3;
    ivec4 dum4;
    ivec4 dum5;
};