#version 460
#extension GL_ARB_separate_shader_objects : enable

layout (location = 0) in vec2 inTexCoords;
layout (location = 1) flat in uint textureIndex;
layout (location = 2) flat in float inSideLight;
layout (location = 3) flat in uint inShadowMapIndex;
layout (location = 4) in vec3 inFragPos;
layout (location = 5) in vec3 inNormal;
layout (location = 6) in vec4 inFragPosLightSpace;
layout (location = 7) flat in vec3 inLightSourcePos;
layout (location = 8) flat in vec3 inViewPos;
layout (location = 9) flat in vec2 inNearFar;

layout (set = 0, binding = 1) uniform texture2DArray texturesOnion[16];
layout (set = 0, binding = 1) uniform texture2D depthTexture[16];
layout (set = 0, binding = 2) uniform sampler samplers[4];

layout (location = 0) out vec4 outColor;


float linearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * inNearFar.x * inNearFar.y) / (inNearFar.y + inNearFar.x - z * (inNearFar.y - inNearFar.x));
}

void main() {
    vec3 lightColor = vec3(1.0);
    vec3 ambient = lightColor * 0.15;
    vec3 normal = normalize(inNormal);
    vec3 lightDir = normalize(inLightSourcePos - inFragPos);
    float diff = max(dot(lightDir, normal), 0.0);
    vec3 diffuse = lightColor * diff;
    vec3 viewDir = normalize(inViewPos - inFragPos);
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(normal, halfwayDir), 0.0), 64.0);
    vec3 specular = lightColor * spec;

    vec4 worldColor = texture(sampler2DArray(texturesOnion[0], samplers[0]), vec3(inTexCoords, float(textureIndex)));

    // SHADOW CALC
    vec3 projCoords = inFragPosLightSpace.xyz / inFragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;
    float currentDepth = projCoords.z;
    float closestDepth = texture(sampler2D(depthTexture[inShadowMapIndex], samplers[2]), projCoords.xy, 1.0).r;
    //closestDepth = linearizeDepth(closestDepth) / inNearFar.y;
    //float dist = distance(inFragPosLightSpace.xyz, inLightSourcePos.xyz);
    //float bias = max(0.05 * (1.0 - dot(normal, lightDir)), 0.005);
    //float bias = (dist / (inNearFar.y - inNearFar.x) * 1.34);
    float bias = closestDepth * 1.2 + max(0.05 * (1.0 - dot(normal, lightDir)), 0.005);
    float shadow = (currentDepth - bias) > closestDepth ? 1.0 : 0.0;
    if (projCoords.z > 1.0) shadow = 0.0;

    vec3 lighting = (ambient + (1.0 - shadow) * (diff + specular)) * worldColor.xyz;
    //vec3 lighting = (clamp(1.0 - shadow, 0.2, 1.0)) * worldColor.xyz;
    //vec3 lighting = worldColor.xyz * ((1.0 - shadow));

    //outColor = vec4(currentDepth, 0.0, 0.0 ,1.0);
    outColor = vec4(lighting, 1.0);
    //outColor = vec4(projCoords, 1.0);
    //outColor = vec4(clamp(inNormal.z, 0.0, 1.0), 0.0, 0.0, 1.0);
    //outColor.xyz *= inSideLight;
}
