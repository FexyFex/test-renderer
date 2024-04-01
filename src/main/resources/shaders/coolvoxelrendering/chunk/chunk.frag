#version 460
#extension GL_ARB_separate_shader_objects : enable

layout (location = 0) in vec2 inTexCoords;
layout (location = 1) flat in uint textureIndex;
layout (location = 2) flat in float inSideLight;
layout (location = 3) flat in uint inShadowMapIndex;
layout (location = 4) in vec3 inFragPos;
layout (location = 5) in vec3 inNormal;
layout (location = 6) in vec4 inLightSpaceFragPos;
layout (location = 7) flat in vec3 inLightSourcePos;
layout (location = 8) flat in vec3 inViewPos;

layout (set = 0, binding = 1) uniform texture2DArray texturesOnion[16];
layout (set = 0, binding = 1) uniform texture2D depthTexture[16];
layout (set = 0, binding = 2) uniform sampler samplers[4];

layout (location = 0) out vec4 outColor;


void main() {
    vec3 lightColor = vec3(1.0);
    vec3 ambient = lightColor * 0.25;
    vec3 lightDir = normalize(inLightSourcePos - inFragPos);
    float diff = max(dot(lightDir, inNormal), 0.0);
    vec3 diffuse = lightColor * diff;
    vec3 viewDir = normalize(inViewPos - inFragPos);
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(inNormal, halfwayDir), 0.0), 64.0);
    vec3 specular = lightColor * spec;

    vec4 worldColor = texture(sampler2DArray(texturesOnion[0], samplers[0]), vec3(inTexCoords, float(textureIndex)));

    // SHADOW CALC
    vec4 fragPosLightSpace = inLightSpaceFragPos;

    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;
    float closestDepth = 1.0 - texture(sampler2D(depthTexture[inShadowMapIndex], samplers[0]), projCoords.xy, 1.0).r;
    float currentDepth = projCoords.z;
    float shadow = currentDepth > closestDepth ? 1.0 : 0.0;

    vec3 lighting = (ambient + (1.0 - shadow) * (diff + specular)) * worldColor.xyz;
    //vec3 lighting = worldColor.xyz * (shadow);

    //outColor = vec4(currentDepth, 0.0, 0.0 ,1.0);
    outColor = vec4(lighting, 1.0);
    //outColor = vec4(projCoords, 1.0);
    //outColor.xyz *= inSideLight;
}
