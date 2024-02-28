#define INPUT_UP_BIT 1
#define INPUT_DOWN_BIT 2
#define INPUT_LEFT_BIT 4
#define INPUT_RIGHT_BIT 8
#define INPUT_SLOW_BIT 16
#define INPUT_SHOOT_BIT 32
#define INPUT_SPELL_BIT 64
#define INPUT_PAUSE_BIT 128

#define PLAYER_SLOW_BIT 1
#define PLAYER_HIT_BIT 2
#define PLAYER_DEAD_BIT 4
#define PLAYER_INVINCIBLE_BIT 8

struct PlayerInfo {
    vec2 position;
    uint health;
    uint spellCount;
    uint flagBits;
    ivec3 placeHolders;
};

struct WorldInfo {
    vec2 cameraPosition;
    vec2 cameraExtent;
    float time;
    float delta;
};

struct BulletData {
    vec2 position;
    float rotation;
    float lifetime;
    float timeLived;
    uint visualID;
    uint behaviourID;
    vec2 behaviourData; // interpretation of this data depends on behaviourID
};
