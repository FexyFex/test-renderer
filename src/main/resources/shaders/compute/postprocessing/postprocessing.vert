#version 450
#extension GL_ARB_separate_shader_objects : enable


layout(push_constant) uniform PushConstants{
    int dummy;
};

layout (location = 0) out vec2 outTexCoords;

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
}