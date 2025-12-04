package com.camille.steply

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.camille.steply.ui.theme.SteplyTheme
import com.camille.steply.pages.Home


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SteplyTheme {
                Home()
            }
        }
    }
}