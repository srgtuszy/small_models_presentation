package demo.functiongemma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import demo.functiongemma.llm.setAndroidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAndroidContext(applicationContext)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}