#version 450
#extension GL_ARB_separate_shader_objects : enable

struct ScreenInfo {
    ivec2 screenExtent;
};

const int ALIGNMENT_LEFT = 1;
const int ALIGNMENT_RIGHT = 2;
const int ALIGNMENT_CENTERED = 4;
const int ALIGNMENT_TOP = 8;
const int ALIGNMENT_BOTTOM = 16;


layout (set = 0, binding = 0) uniform UBO { ScreenInfo screenInfo; } screenInfoBuffer;

layout (location = 0) in vec4 inPosition;
layout (location = 1) in vec4 inTexCoords;

layout(push_constant) uniform PushConstants{
    ivec2 position;
    ivec2 extent;
    int zLayer;
    int alignmentFlags;
    int textureIndex;
};

layout (location = 0) out vec2 outTexCoords;
layout (location = 1) flat out int outTexIndex;


void main() {
    ScreenInfo screenInfo = screenInfoBuffer.screenInfo;

    vec2 screenSize = vec2(screenInfo.screenExtent);
    vec2 extentRatio = extent / screenSize;
    vec2 transformedPos = position / (screenSize / 2.0);
    vec2 actualPosition = (inPosition.xy * (extentRatio * 2.0)) + transformedPos;

    // Check alignments here
    int left = int((ALIGNMENT_LEFT & alignmentFlags) == ALIGNMENT_LEFT);
    int right = int((ALIGNMENT_RIGHT & alignmentFlags) == ALIGNMENT_RIGHT);
    int top = int((ALIGNMENT_TOP & alignmentFlags) == ALIGNMENT_TOP);
    int bottom = int((ALIGNMENT_BOTTOM & alignmentFlags) == ALIGNMENT_BOTTOM);
    int centered = int((ALIGNMENT_CENTERED & alignmentFlags) == ALIGNMENT_CENTERED);

    // Apply alignment position translation
    actualPosition.x -= left;
    actualPosition.x += right - (right * extentRatio.x * 2.0);
    actualPosition.y -= top;
    actualPosition.y += bottom - (bottom * extentRatio.y * 2.0);
    actualPosition -= centered * (extentRatio * (vec2(right ^ 1, bottom ^ 1) - vec2(left, top)));
    // ^ is xor. used to flip between 0 and 1

    float actualZlayer = zLayer / 100.0;

    gl_Position = vec4(actualPosition, actualZlayer, 1.0);
    outTexCoords = inTexCoords.xy;
    outTexIndex = textureIndex;
}