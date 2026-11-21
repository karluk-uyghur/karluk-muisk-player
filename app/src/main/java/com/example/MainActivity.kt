package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.designsystem.KarlukTheme
import com.example.core.navigation.KarlukNavHost
import com.example.data.datastore.UserPreferencesRepository
import com.example.localization.LocaleHelper
import com.example.playback.service.KarlukPlaybackService
import com.example.ui.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as KarlukMp3ProApp).container)
    }

    private var openNowPlayingSignal by mutableIntStateOf(0)

    override fun attachBaseContext(newBase: Context) {
        val savedLanguage = UserPreferencesRepository(newBase).getSavedLanguageSync()
        super.attachBaseContext(LocaleHelper.wrapContext(newBase, savedLanguage))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
            val layoutDirection = LocaleHelper.layoutDirectionFor(userSettings.language)

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                KarlukTheme(settings = userSettings) {
                    KarlukNavHost(
                        viewModel = viewModel,
                        openNowPlayingSignal = openNowPlayingSignal,
                        onLanguageChanged = { language ->
                            viewModel.setLanguage(language) {
                                recreate()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(KarlukPlaybackService.EXTRA_OPEN_NOW_PLAYING, false)) {
            openNowPlayingSignal++
        }
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                viewModel.playExternalUri(uri)
                openNowPlayingSignal++
            }
        }
    }
}
