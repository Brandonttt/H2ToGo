package com.htogo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.htogo.app.navigation.HToGoNavHost
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { HToGoRoot() }
    }
}

@Composable
private fun HToGoRoot() {
    HToGoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = HToGoColors.Background
        ) {
            val navController = rememberNavController()
            HToGoNavHost(navController = navController)
        }
    }
}
