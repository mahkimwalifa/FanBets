package com.seamhealth.elsrt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.seamhealth.elsrt.ui.navigation.FanBetsNavHost
import com.seamhealth.elsrt.ui.theme.FanBetsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FanBetsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FanBetsNavHost()
                }
            }
        }
    }
}
