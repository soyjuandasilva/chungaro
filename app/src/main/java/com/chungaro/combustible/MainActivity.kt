package com.chungaro.combustible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chungaro.combustible.ui.ChungaroApp
import com.chungaro.combustible.ui.ChungaroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChungaroTheme {
                ChungaroApp()
            }
        }
    }
}
