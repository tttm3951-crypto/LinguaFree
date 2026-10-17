package org.linguafree.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import org.linguafree.app.navigation.LinguaFreeRoot

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val root: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the fox splash while course content is imported on first launch.
        splash.setKeepOnScreenCondition { root.state.value == StartupState.Loading || root.settings.value == null }
        enableEdgeToEdge()
        setContent { LinguaFreeRoot(root) }
    }

    override fun onResume() {
        super.onResume()
        root.onResume()
    }
}
