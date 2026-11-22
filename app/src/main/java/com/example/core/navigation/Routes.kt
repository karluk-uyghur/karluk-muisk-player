package com.example.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable
    data object Splash : AppRoute

    @Serializable
    data object Onboarding : AppRoute

    @Serializable
    data object MainPager : AppRoute

    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Library : AppRoute

    @Serializable
    data object Favorites : AppRoute

    @Serializable
    data object AddFiles : AppRoute

    @Serializable
    data object Settings : AppRoute

    @Serializable
    data object NowPlaying : AppRoute

    @Serializable
    data object Lyrics : AppRoute

    @Serializable
    data object Equalizer : AppRoute

    @Serializable
    data object Search : AppRoute

    @Serializable
    data object Queue : AppRoute

    @Serializable
    data object History : AppRoute

    @Serializable
    data object SleepTimer : AppRoute

    @Serializable
    data object ThemeStudio : AppRoute

    @Serializable
    data object StorageInfo : AppRoute

    @Serializable
    data object About : AppRoute

    @Serializable
    data class PlaylistDetail(val playlistId: Long) : AppRoute

    @Serializable
    data class AlbumDetail(val albumName: String) : AppRoute

    @Serializable
    data class ArtistDetail(val artistName: String) : AppRoute

    @Serializable
    data class FolderDetail(val folderPath: String, val folderName: String) : AppRoute
}
