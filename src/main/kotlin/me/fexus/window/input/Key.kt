package me.fexus.window.input

import org.lwjgl.glfw.GLFW.*


enum class Key(val glfwValue: Int) {
    `0`(GLFW_KEY_0),
    `1`(GLFW_KEY_1),
    `2`(GLFW_KEY_2),
    `3`(GLFW_KEY_3),
    `4`(GLFW_KEY_4),
    `5`(GLFW_KEY_5),
    `6`(GLFW_KEY_6),
    `7`(GLFW_KEY_7),
    `8`(GLFW_KEY_8),
    `9`(GLFW_KEY_9),

    Q(GLFW_KEY_Q),
    W(GLFW_KEY_W),
    E(GLFW_KEY_E),
    R(GLFW_KEY_R),
    T(GLFW_KEY_T),
    Z(GLFW_KEY_Z),
    U(GLFW_KEY_U),
    I(GLFW_KEY_I),
    O(GLFW_KEY_O),
    P(GLFW_KEY_P),
    A(GLFW_KEY_A),
    S(GLFW_KEY_S),
    D(GLFW_KEY_D),
    F(GLFW_KEY_F),
    G(GLFW_KEY_G),
    H(GLFW_KEY_H),
    J(GLFW_KEY_J),
    K(GLFW_KEY_K),
    L(GLFW_KEY_L),
    Y(GLFW_KEY_Y),
    X(GLFW_KEY_X),
    C(GLFW_KEY_C),
    V(GLFW_KEY_V),
    B(GLFW_KEY_B),
    N(GLFW_KEY_N),
    M(GLFW_KEY_M),

    SPACE(GLFW_KEY_SPACE),
    LSHIFT(GLFW_KEY_LEFT_SHIFT),
    TAB(GLFW_KEY_TAB),
    ENTER(GLFW_KEY_ENTER),
    BACKSPACE(GLFW_KEY_BACKSPACE),
    ESC(GLFW_KEY_ESCAPE),

    ARROW_LEFT(GLFW_KEY_LEFT),
    ARROW_RIGHT(GLFW_KEY_RIGHT),
    ARROW_UP(GLFW_KEY_UP),
    ARROW_DOWN(GLFW_KEY_DOWN),

    PAGE_UP(GLFW_KEY_PAGE_UP),
    PAGE_DOWN(GLFW_KEY_PAGE_DOWN),
    END(GLFW_KEY_END)
    ;


    companion object {
        fun getKeyByValue(keyVal: Int): Key? {
            return values().find { it.glfwValue == keyVal }
        }
    }
}