#version 450
#extension GL_ARB_separate_shader_objects : enable

const int DIFFUSE_MAP_INDEX = 0;
const int DISPLACEMENT_MAP_INDEX= 1;
const int NORMAL_MAP_INDEX = 2;

layout (location = 0) in vec3 inFragPos;
layout (location = 1) in vec2 inTexCoords;
layout (location = 2) in vec3 inTangentLightPos;
layout (location = 3) in vec3 inTangentViewPos;
layout (location = 4) in vec3 inTangentFragPos;
layout (location = 5) in vec3 inNormal;
layout (location = 6) in mat3 inTBN;

layout (set = 0, binding = 1) uniform texture2DArray maps;
layout (set = 0, binding = 2) uniform sampler defaultSampler;

layout(push_constant) uniform PushConstants{
    mat4 modelMatrix;
    vec4 lightPos;
    vec4 viewPos;
    float heightScale;
};

layout (location = 0) out vec4 outColor;


float fetchHeight(vec2 texCoords) {
    return texture(sampler2DArray(maps, defaultSampler), vec3(texCoords, DISPLACEMENT_MAP_INDEX)).r;
}

vec2 parallaxMapping(vec2 texCoords, vec3 viewDir) {
    const float numLayers = 32;
    float layerDepth = 1.0 / numLayers;
    float currentLayerDepth = 0.0;

    vec2 p = viewDir.xy * heightScale;
    vec2 deltaTexCoords = p / numLayers;

    vec2 currentTexCoords = texCoords;
    float currentDepthMapValue = fetchHeight(currentTexCoords);

    while (currentLayerDepth < currentDepthMapValue) {
        currentTexCoords -= deltaTexCoords;
        currentDepthMapValue = fetchHeight(currentTexCoords);
        currentLayerDepth += layerDepth;
    }

    return currentTexCoords;
}

void main() {
    vec3 viewDir = normalize(inTangentViewPos - inTangentFragPos);
    vec2 texCoords = parallaxMapping(inTexCoords, viewDir);
    if(texCoords.x > 1.0 || texCoords.y > 1.0 || texCoords.x < 0.0 || texCoords.y < 0.0) discard;

    vec4 diffTex = texture(sampler2DArray(maps, defaultSampler), vec3(texCoords, DIFFUSE_MAP_INDEX));
    vec4 normTex = texture(sampler2DArray(maps, defaultSampler), vec3(texCoords, NORMAL_MAP_INDEX));

    vec3 normal = normalize(normTex.rgb * 2.0 - 1.0);
    vec3 lightDir = inTBN * normalize(lightPos.xyz - inFragPos);
    vec3 reflectDir = reflect(-lightDir, normal);

    float diffuseLight = max(dot(normalize(inNormal), lightDir), 0.0);
    float specularLight = pow(max(dot(viewDir, reflectDir), 0.0), 16.0) * 0.5;

    vec3 finalColor = diffTex.xyz * (diffuseLight + specularLight);
    outColor = vec4(finalColor, 1.0);
}
