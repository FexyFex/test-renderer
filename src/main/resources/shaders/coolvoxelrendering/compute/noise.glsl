#include "./perlin_noise.glsl"
#include "./simplex_noise.glsl"


float perlin_noise2D(vec2 pos, float gain, float lacunarity, float fractalBounding, float weightedStrength, float frequency, int octaves) {
    vec2 lPos = pos * frequency;

    float sum = 0;
    float amp = fractalBounding;

    for (int i = 0; i < octaves; i++) {
        float noise = cnoise_perlin(lPos);
        sum += noise * amp;
        amp *= mix(1.0, min(noise + 1.0, 2.0) * 0.5, weightedStrength);

        lPos.x *= lacunarity;
        lPos.y *= lacunarity;
        amp *= gain;
    }

    return sum;
}

float perlin_noise3D(vec3 pos, float gain, float lacunarity, float fractalBounding, float weightedStrength, float frequency, int octaves) {
    vec3 lPos = pos * frequency;

    float sum = 0;
    float amp = fractalBounding;

    for (int i = 0; i < octaves; i++) {
        float noise = cnoise_perlin(lPos);
        sum += noise * amp;
        amp *= mix(1.0, min(noise + 1.0, 2.0) * 0.5, weightedStrength);

        lPos.x *= lacunarity;
        lPos.y *= lacunarity;
        lPos.z *= lacunarity;
        amp *= gain;
    }

    return sum;
}

float simplex_noise3D(vec3 pos, float gain, float lacunarity, float fractalBounding, float weightedStrength, float frequency, int octaves) {
    vec3 lPos = pos * frequency;

    float sum = 0;
    float amp = fractalBounding;

    for (int i = 0; i < octaves; i++) {
        float noise = snoise_simplex(lPos);
        sum += noise * amp;
        amp *= mix(1.0, min(noise + 1.0, 2.0) * 0.5, weightedStrength);

        lPos.x *= lacunarity;
        lPos.y *= lacunarity;
        lPos.z *= lacunarity;
        amp *= gain;
    }

    return sum;
}

float simplex_noise2D(vec2 pos, float gain, float lacunarity, float fractalBounding, float weightedStrength, float frequency, int octaves) {
    vec2 lPos = pos * frequency;

    float sum = 0;
    float amp = fractalBounding;

    for (int i = 0; i < octaves; i++) {
        float noise = snoise_simplex(lPos);
        sum += noise * amp;
        amp *= mix(1.0, min(noise + 1.0, 2.0) * 0.5, weightedStrength);

        lPos.x *= lacunarity;
        lPos.y *= lacunarity;
        amp *= gain;
    }

    return sum;
}