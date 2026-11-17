package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.core.LeviLogger
import com.example.ui.LeviApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LeviLogger.getInstance(applicationContext)
        enableEdgeToEdge()
        setContent {
            LeviApp()
        }
    }
}
