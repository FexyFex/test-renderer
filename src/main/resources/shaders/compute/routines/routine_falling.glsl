
vec2 fall(vec2 initialPosition, vec2 initialVelocity, vec2 fallDir, uint lifetime) {
    return initialPosition + (initialVelocity * lifetime) + (lifetime * lifetime * (fallDir / 2));
}