package demo.functiongemma

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Function Gemma Demo"
    ) {
        App()
    }
}