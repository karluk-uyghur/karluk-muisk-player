package com.example

import android.app.Application
import android.content.Context
import com.example.data.database.KarlukDatabase
import com.example.data.datastore.UserPreferencesRepository
import com.example.data.media.MediaScanner
import com.example.data.repository.MusicRepository
import com.example.playback.audio.KarlukAudioProcessor
import com.example.playback.controller.PlaybackController
import com.example.playback.effects.AudioEffectsManager

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val database = KarlukDatabase.getInstance(appContext)
    val preferencesRepository = UserPreferencesRepository(appContext)
    val mediaScanner = MediaScanner(appContext)
    val musicRepository = MusicRepository(
        context = appContext,
        dao = database.karlukDao(),
        mediaScanner = mediaScanner
    )
    val audioProcessor = KarlukAudioProcessor()
    val audioEffectsManager = AudioEffectsManager(audioProcessor)
    val playbackController = PlaybackController(
        context = appContext,
        musicRepository = musicRepository,
        preferencesRepository = preferencesRepository
    )
}

class KarlukMp3ProApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
