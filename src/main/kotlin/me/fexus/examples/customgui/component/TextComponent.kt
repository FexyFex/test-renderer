package me.fexus.examples.customgui.component

abstract class TextComponent(initialText: String): TexturedComponent() {
    var text: String = initialText
        set(value) {
            field = value
            textRequiresUpdate = true
        }
    var textRequiresUpdate: Boolean = true
}