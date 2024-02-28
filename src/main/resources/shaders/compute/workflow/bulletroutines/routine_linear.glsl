vec2 linear(vec2 currentPosition, vec2 velocity, float delta) {
    return currentPosition + (velocity * delta);
}