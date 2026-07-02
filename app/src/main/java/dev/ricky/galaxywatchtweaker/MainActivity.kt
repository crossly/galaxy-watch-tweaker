package dev.ricky.galaxywatchtweaker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dev.ricky.galaxywatchtweaker.settings.TweakerSettingsViewModel
import dev.ricky.galaxywatchtweaker.ui.GalaxyWatchTweakerApp
import dev.ricky.galaxywatchtweaker.ui.theme.GalaxyWatchTweakerTheme

class MainActivity : AppCompatActivity() {
    private val viewModel: TweakerSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GalaxyWatchTweakerTheme {
                GalaxyWatchTweakerApp(viewModel = viewModel)
            }
        }
    }
}
