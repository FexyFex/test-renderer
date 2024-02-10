vec2 linear(vec2 initialPosition, vec2 velocity, uint timeLived) {
    return initialPosition + (velocity * timeLived);
}