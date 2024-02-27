package me.fexus.examples.compute.bulletlimbo

enum class InputCode(bit: Int) {
    UP(1),
    DOWN(2),
    LEFT(4),
    RIGHT(8),
    SLOW(16),
    SHOOT(32),
    SPELL(64),
    PAUSE(128),
    CAMERA_PAN_UP(256),
    CAMERA_PAN_DOWN(512),
    CAMERA_PAN_LEFT(1024),
    CAMERA_PAN_RIGHT(2048)
}