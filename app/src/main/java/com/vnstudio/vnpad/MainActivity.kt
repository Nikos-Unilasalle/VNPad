package com.vnstudio.vnpad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vnstudio.vnpad.ui.VNPadApp
import com.vnstudio.vnpad.ui.theme.VNPadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            VNPadTheme { VNPadApp() }
        }
    }
}
