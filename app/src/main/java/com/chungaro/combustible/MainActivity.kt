package com.chungaro.combustible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chungaro.combustible.ui.ChungaroApp
import com.chungaro.combustible.ui.ChungaroTheme
import com.chungaro.combustible.ui.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // El mismo ViewModel que usa la app: el tema cambia en cuanto se toca en Definições.
            val vm: AppViewModel = viewModel()
            ChungaroTheme(mode = vm.settings.themeMode) {
                ChungaroApp(vm = vm)
            }
        }
    }
}
