package com.example.core.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.core.designsystem.KarlukBackground
import com.example.core.designsystem.LocalKarlukTokens
import com.example.core.designsystem.MiniPlayerBar
import com.example.domain.model.AppLanguage
import com.example.feature.equalizer.EqualizerScreen
import com.example.feature.favorites.FavoritesScreen
import com.example.feature.files.AddFilesScreen
import com.example.feature.home.HomeDashboardScreen
import com.example.feature.library.CollectionDetailScreen
import com.example.feature.library.LibraryScreen
import com.example.feature.lyrics.LyricsScreen
import com.example.feature.nowplaying.NowPlayingScreen
import com.example.feature.queue.HistoryScreen
import com.example.feature.queue.QueueScreen
import com.example.feature.queue.SleepTimerScreen
import com.example.feature.search.SearchScreen
import com.example.feature.settings.AboutScreen
import com.example.feature.settings.SettingsScreen
import com.example.feature.settings.StorageInfoScreen
import com.example.feature.settings.ThemeStudioScreen
import com.example.feature.splash.OnboardingScreen
import com.example.feature.splash.SplashScreen
import com.example.ui.MainViewModel

private data class BottomTabItem(
    val route: AppRoute,
    val icon: ImageVector,
    val labelRes: Int,
    val testTag: String
)

@Composable
fun KarlukNavHost(
    viewModel: MainViewModel,
    openNowPlayingSignal: Int,
    onLanguageChanged: (AppLanguage) -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokens = LocalKarlukTokens.current
    val snackbarHostState = remember { SnackbarHostState() }

    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val historySongs by viewModel.historySongs.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val safFolders by viewModel.safFolders.collectAsStateWithLifecycle()
    val excludedFolders by viewModel.excludedFolders.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val audioEffectsState by viewModel.audioEffectsState.collectAsStateWithLifecycle()
    val spectrum by viewModel.spectrumFlow.collectAsStateWithLifecycle()
    val currentLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsToRequest = buildList {
        add(audioPermission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[audioPermission] == true
        hasAudioPermission = granted
        if (granted) {
            viewModel.rescanMedia()
        }
    }

    val onboardingSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.addSafFolder(uri)
            viewModel.setOnboardingCompleted(true)
            navController.navigate(AppRoute.Home) {
                popUpTo(AppRoute.Onboarding) { inclusive = true }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(openNowPlayingSignal) {
        if (openNowPlayingSignal > 0 && playbackState.currentSong != null) {
            navController.navigate(AppRoute.NowPlaying)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val topLevelTabs = remember {
        listOf(
            BottomTabItem(AppRoute.Home, Icons.Rounded.Home, R.string.nav_home, "nav_home"),
            BottomTabItem(AppRoute.Library, Icons.Rounded.LibraryMusic, R.string.nav_library, "nav_library"),
            BottomTabItem(AppRoute.Favorites, Icons.Rounded.Favorite, R.string.nav_favorites, "nav_favorites"),
            BottomTabItem(AppRoute.AddFiles, Icons.Rounded.CreateNewFolder, R.string.nav_add_files, "nav_add_files"),
            BottomTabItem(AppRoute.Settings, Icons.Rounded.Settings, R.string.nav_settings, "nav_settings")
        )
    }

    val isTopLevelRoute = topLevelTabs.any { tab ->
        currentDestination?.hasRoute(tab.route::class) == true
    }
    val isHomeRoute = currentDestination?.hasRoute(AppRoute.Home::class) == true

    fun navigateTopLevel(route: AppRoute) {
        navController.navigate(route) {
            popUpTo(AppRoute.Home) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        KarlukBackground {
            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail && isTopLevelRoute) {
                    NavigationRail(
                        containerColor = tokens.glassSurfaceElevated,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .border(0.8.dp, tokens.glassBorder)
                    ) {
                        Spacer(Modifier.weight(1f))
                        topLevelTabs.forEach { tab ->
                            val selected = currentDestination?.hasRoute(tab.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = tokens.accentPrimary,
                                    selectedTextColor = tokens.accentPrimary,
                                    unselectedIconColor = tokens.textSecondary,
                                    unselectedTextColor = tokens.textSecondary,
                                    indicatorColor = tokens.accentPrimary.copy(alpha = 0.16f)
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }

                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (isTopLevelRoute) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (!useRail) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier)
                            ) {
                                val activeSong = playbackState.currentSong
                                if (!isHomeRoute && activeSong != null) {
                                    MiniPlayerBar(
                                        song = activeSong,
                                        isPlaying = playbackState.isPlaying,
                                        positionMs = playbackState.positionMs,
                                        durationMs = playbackState.durationMs,
                                        onOpenNowPlaying = { navController.navigate(AppRoute.NowPlaying) },
                                        onTogglePlayPause = { viewModel.togglePlayPause() },
                                        onNext = { viewModel.skipToNext() },
                                        onOpenQueue = { navController.navigate(AppRoute.Queue) }
                                    )
                                }

                                if (!useRail) {
                                    NavigationBar(
                                        containerColor = tokens.glassSurfaceElevated.copy(alpha = 0.92f),
                                        tonalElevation = 0.dp,
                                        modifier = Modifier.border(0.6.dp, tokens.glassBorder)
                                    ) {
                                        topLevelTabs.forEach { tab ->
                                            val selected = currentDestination?.hasRoute(tab.route::class) == true
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { navigateTopLevel(tab.route) },
                                                icon = {
                                                    Icon(
                                                        imageVector = tab.icon,
                                                        contentDescription = stringResource(tab.labelRes)
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = stringResource(tab.labelRes),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = tokens.accentPrimary,
                                                    selectedTextColor = tokens.accentPrimary,
                                                    unselectedIconColor = tokens.textSecondary,
                                                    unselectedTextColor = tokens.textSecondary,
                                                    indicatorColor = tokens.accentPrimary.copy(alpha = 0.16f)
                                                ),
                                                modifier = Modifier.testTag(tab.testTag)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = AppRoute.Splash
                        ) {
                            composable<AppRoute.Splash> {
                                SplashScreen(
                                    onboardingCompleted = userSettings.onboardingCompleted,
                                    onNavigateToOnboarding = {
                                        navController.navigate(AppRoute.Onboarding) {
                                            popUpTo(AppRoute.Splash) { inclusive = true }
                                        }
                                    },
                                    onNavigateToHome = {
                                        navController.navigate(AppRoute.Home) {
                                            popUpTo(AppRoute.Splash) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable<AppRoute.Onboarding> {
                                OnboardingScreen(
                                    currentLanguage = userSettings.language,
                                    onSelectLanguage = onLanguageChanged,
                                    onRequestAudioPermission = {
                                        permissionLauncher.launch(permissionsToRequest)
                                        viewModel.setOnboardingCompleted(true)
                                        navController.navigate(AppRoute.Home) {
                                            popUpTo(AppRoute.Onboarding) { inclusive = true }
                                        }
                                    },
                                    onPickSafFolder = {
                                        onboardingSafLauncher.launch(null)
                                    },
                                    onContinueOffline = {
                                        viewModel.setOnboardingCompleted(true)
                                        navController.navigate(AppRoute.Home) {
                                            popUpTo(AppRoute.Onboarding) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable<AppRoute.Home> {
                                HomeDashboardScreen(
                                    playbackState = playbackState,
                                    spectrum = spectrum,
                                    allSongsCount = allSongs.size,
                                    recentSongs = historySongs,
                                    onOpenNowPlaying = { navController.navigate(AppRoute.NowPlaying) },
                                    onOpenSearch = { navController.navigate(AppRoute.Search) },
                                    onOpenSettings = { navigateTopLevel(AppRoute.Settings) },
                                    onOpenLibrary = { navigateTopLevel(AppRoute.Library) },
                                    onOpenFavorites = { navigateTopLevel(AppRoute.Favorites) },
                                    onOpenEqualizer = { navController.navigate(AppRoute.Equalizer) },
                                    onOpenAddFiles = { navigateTopLevel(AppRoute.AddFiles) },
                                    onOpenHistory = { navController.navigate(AppRoute.History) },
                                    onOpenSleepTimer = { navController.navigate(AppRoute.SleepTimer) },
                                    onTogglePlayPause = { viewModel.togglePlayPause() },
                                    onSkipNext = { viewModel.skipToNext() },
                                    onSkipPrev = { viewModel.skipToPrevious() },
                                    onSeekTo = { viewModel.seekTo(it) },
                                    onToggleShuffle = { viewModel.toggleShuffle() },
                                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                                    onPlaySong = { list, idx -> viewModel.playSongs(list, idx) }
                                )
                            }

                            composable<AppRoute.Library> {
                                LibraryScreen(
                                    allSongs = allSongs,
                                    albums = albums,
                                    artists = artists,
                                    folders = folders,
                                    playlists = playlists,
                                    historySongs = historySongs,
                                    playbackState = playbackState,
                                    isScanning = isScanning,
                                    hasAudioPermission = hasAudioPermission,
                                    onRequestPermission = { permissionLauncher.launch(permissionsToRequest) },
                                    onRescan = { viewModel.rescanMedia() },
                                    onOpenSearch = { navController.navigate(AppRoute.Search) },
                                    onOpenAddFiles = { navigateTopLevel(AppRoute.AddFiles) },
                                    onOpenAlbum = { name -> navController.navigate(AppRoute.AlbumDetail(name)) },
                                    onOpenArtist = { name -> navController.navigate(AppRoute.ArtistDetail(name)) },
                                    onOpenFolder = { f -> navController.navigate(AppRoute.FolderDetail(f.path, f.name)) },
                                    onOpenPlaylist = { id -> navController.navigate(AppRoute.PlaylistDetail(id)) },
                                    onPlaySongs = { list, idx -> viewModel.playSongs(list, idx) },
                                    onShuffleSongs = { list -> viewModel.shuffleAndPlay(list) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onBatchFavorite = { viewModel.addSongsToFavorites(it) },
                                    onPlayNext = { viewModel.playNext(it) },
                                    onAddToQueue = { viewModel.addToQueue(it) },
                                    onCreatePlaylist = { name, ids -> viewModel.createPlaylist(name, ids) },
                                    onRenamePlaylist = { pid, name -> viewModel.renamePlaylist(pid, name) },
                                    onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                    onAddToPlaylist = { pid, ids -> viewModel.addSongsToPlaylist(pid, ids) },
                                    onRemoveFromLibrary = { viewModel.removeSongFromLibrary(it) }
                                )
                            }

                            composable<AppRoute.Favorites> {
                                FavoritesScreen(
                                    favoriteSongs = favoriteSongs,
                                    playlists = playlists,
                                    playbackState = playbackState,
                                    onPlaySongs = { list, idx -> viewModel.playSongs(list, idx) },
                                    onShuffleSongs = { list -> viewModel.shuffleAndPlay(list) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onPlayNext = { viewModel.playNext(it) },
                                    onAddToQueue = { viewModel.addToQueue(it) },
                                    onAddToPlaylist = { pid, ids -> viewModel.addSongsToPlaylist(pid, ids) },
                                    onCreatePlaylist = { name, ids -> viewModel.createPlaylist(name, ids) },
                                    onRemoveFromLibrary = { viewModel.removeSongFromLibrary(it) }
                                )
                            }

                            composable<AppRoute.AddFiles> {
                                AddFilesScreen(
                                    safFolders = safFolders,
                                    excludedFolders = excludedFolders,
                                    deviceFolders = folders,
                                    storageStats = storageStats,
                                    isScanning = isScanning,
                                    hasAudioPermission = hasAudioPermission,
                                    onRequestPermission = { permissionLauncher.launch(permissionsToRequest) },
                                    onRescanMedia = { viewModel.rescanMedia() },
                                    onImportAudioFiles = { uris -> viewModel.importAudioFiles(uris, playImmediately = true) },
                                    onAddSafFolder = { uri -> viewModel.addSafFolder(uri) },
                                    onRemoveSafFolder = { viewModel.removeSafFolder(it) },
                                    onRestoreExcludedFolder = { viewModel.restoreFolder(it) },
                                    onOpenFolderDetail = { f -> navController.navigate(AppRoute.FolderDetail(f.path, f.name)) },
                                    onOpenStorageInfo = { navController.navigate(AppRoute.StorageInfo) }
                                )
                            }

                            composable<AppRoute.Settings> {
                                SettingsScreen(
                                    settings = userSettings,
                                    onSelectLanguage = onLanguageChanged,
                                    onOpenThemeStudio = { navController.navigate(AppRoute.ThemeStudio) },
                                    onOpenEqualizer = { navController.navigate(AppRoute.Equalizer) },
                                    onOpenSleepTimer = { navController.navigate(AppRoute.SleepTimer) },
                                    onOpenStorageInfo = { navController.navigate(AppRoute.StorageInfo) },
                                    onOpenAbout = { navController.navigate(AppRoute.About) },
                                    onToggleResumePlayback = { viewModel.setResumePlayback(it) },
                                    onToggleGapless = { viewModel.setGaplessPlayback(it) },
                                    onToggleAutoScan = { viewModel.setAutoScanOnStartup(it) },
                                    onClearHistory = { viewModel.clearHistory() }
                                )
                            }

                            composable<AppRoute.NowPlaying> {
                                NowPlayingScreen(
                                    playbackState = playbackState,
                                    spectrum = spectrum,
                                    onBack = { navController.popBackStack() },
                                    onTogglePlayPause = { viewModel.togglePlayPause() },
                                    onSkipNext = { viewModel.skipToNext() },
                                    onSkipPrev = { viewModel.skipToPrevious() },
                                    onSeekTo = { viewModel.seekTo(it) },
                                    onToggleShuffle = { viewModel.toggleShuffle() },
                                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                                    onChangeSpeed = { viewModel.setPlaybackSpeed(it) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onRetry = { viewModel.retryPlayback() },
                                    onOpenLyrics = { navController.navigate(AppRoute.Lyrics) },
                                    onOpenQueue = { navController.navigate(AppRoute.Queue) },
                                    onOpenEqualizer = { navController.navigate(AppRoute.Equalizer) },
                                    onOpenSleepTimer = { navController.navigate(AppRoute.SleepTimer) }
                                )
                            }

                            composable<AppRoute.Lyrics> {
                                LyricsScreen(
                                    playbackState = playbackState,
                                    lyricsData = currentLyrics,
                                    onBack = { navController.popBackStack() },
                                    onSeekTo = { viewModel.seekTo(it) },
                                    onSaveLyrics = { id, content -> viewModel.saveLyrics(id, content) },
                                    onImportLrcFile = { id, uri -> viewModel.importLrcFile(id, uri) }
                                )
                            }

                            composable<AppRoute.Equalizer> {
                                EqualizerScreen(
                                    effectsState = audioEffectsState,
                                    spectrum = spectrum,
                                    isPlaying = playbackState.isPlaying,
                                    onBack = { navController.popBackStack() },
                                    onToggleEqualizer = { viewModel.setEqualizerEnabled(it) },
                                    onSetBandLevel = { idx, lvl -> viewModel.setEqualizerBand(idx, lvl) },
                                    onApplyPreset = { viewModel.applyEqualizerPreset(it) },
                                    onSetBassBoost = { en, str -> viewModel.setBassBoost(en, str) },
                                    onSetVirtualizer = { en, str -> viewModel.setVirtualizer(en, str) },
                                    onSetLoudness = { en, gain -> viewModel.setLoudnessEnhancer(en, gain) },
                                    onSetBalance = { viewModel.setBalance(it) }
                                )
                            }

                            composable<AppRoute.Search> {
                                SearchScreen(
                                    allSongs = allSongs,
                                    playlists = playlists,
                                    searchHistory = searchHistory,
                                    playbackState = playbackState,
                                    onBack = { navController.popBackStack() },
                                    onSaveSearchQuery = { viewModel.saveSearchQuery(it) },
                                    onClearSearchHistory = { viewModel.clearSearchHistory() },
                                    onPlaySongs = { list, idx -> viewModel.playSongs(list, idx) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onPlayNext = { viewModel.playNext(it) },
                                    onAddToQueue = { viewModel.addToQueue(it) },
                                    onAddToPlaylist = { pid, ids -> viewModel.addSongsToPlaylist(pid, ids) },
                                    onCreatePlaylist = { name, ids -> viewModel.createPlaylist(name, ids) },
                                    onRemoveFromLibrary = { viewModel.removeSongFromLibrary(it) }
                                )
                            }

                            composable<AppRoute.Queue> {
                                QueueScreen(
                                    playbackState = playbackState,
                                    onBack = { navController.popBackStack() },
                                    onSkipToIndex = { viewModel.skipToQueueIndex(it) },
                                    onMoveItem = { from, to -> viewModel.moveQueueItem(from, to) },
                                    onRemoveItem = { viewModel.removeQueueItem(it) },
                                    onClearQueue = { viewModel.clearQueue() },
                                    onSaveQueueAsPlaylist = { name, ids -> viewModel.createPlaylist(name, ids) }
                                )
                            }

                            composable<AppRoute.History> {
                                HistoryScreen(
                                    historySongs = historySongs,
                                    playlists = playlists,
                                    playbackState = playbackState,
                                    onBack = { navController.popBackStack() },
                                    onClearHistory = { viewModel.clearHistory() },
                                    onPlaySongs = { list, idx -> viewModel.playSongs(list, idx) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onPlayNext = { viewModel.playNext(it) },
                                    onAddToQueue = { viewModel.addToQueue(it) },
                                    onAddToPlaylist = { pid, ids -> viewModel.addSongsToPlaylist(pid, ids) },
                                    onCreatePlaylist = { name, ids -> viewModel.createPlaylist(name, ids) },
                                    onRemoveFromLibrary = { viewModel.removeSongFromLibrary(it) }
                                )
                            }

                            composable<AppRoute.SleepTimer> {
                                SleepTimerScreen(
                                    playbackState = playbackState,
                                    onBack = { navController.popBackStack() },
                                    onSetMinutes = { viewModel.setSleepTimerMinutes(it) },
                                    onSetEndOfTrack = { viewModel.setSleepAfterCurrentTrack() },
                                    onCancelTimer = { viewModel.cancelSleepTimer() }
                                )
                            }

                            composable<AppRoute.ThemeStudio> {
                                ThemeStudioScreen(
                                    settings = userSettings,
                                    onBack = { navController.popBackStack() },
                                    onSelectAtmosphere = { viewModel.setThemeAtmosphere(it) },
                                    onSelectAccent = { viewModel.setAccentOption(it) },
                                    onChangeGlassOpacity = { viewModel.setGlassOpacity(it) },
                                    onToggleReducedMotion = { viewModel.setReducedMotion(it) },
                                    onToggleLowPower = { viewModel.setLowPowerMode(it) }
                                )
                            }

                            composable<AppRoute.StorageInfo> {
                                StorageInfoScreen(
                                    stats = storageStats,
                                    onBack = { navController.popBackStack() },
                                    onClearCache = { viewModel.clearArtworkCache() }
                                )
                            }

                            composable<AppRoute.About> {
                                AboutScreen(onBack = { navController.popBackStack() })
                            }

                            composable<AppRoute.PlaylistDetail> { backStackEntry ->
                                val route = backStackEntry.toRoute<AppRoute.PlaylistDetail>()
                                val playlist = playlists.firstOrNull { it.id == route.playlistId }
                                CollectionDetailScreen(
                                    title = playlist?.name ?: stringResource(R.string.tab_playlists),
                                    songs = playlist?.songs.orEmpty(),
                                    playlists = playlists,
                                    playbackState = playbackState,
                                    playlistId = route.playlistId,
                                    onBack = { navController.popBackStack() },
                                    onPlaySongs = { list, idx -> viewModel.playSongs(list, idx) },
                                    onShuffleSongs = { list -> viewModel.shuffleAndPlay(list) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onPlayNext = { viewModel.playNext(it) },
                                    onAddToQueue = { viewModel.addToQueue(it) },
                                    onAddToPlaylist = { pid, ids -> viewModel.addSongsToPlaylist(pid, ids) },
                                    onCreatePlaylist = { name, ids -> viewModel.createPlaylist(name, ids) },
                                    onRemoveFromPlaylist = { songId -> viewModel.removeSongFromPlaylist(route.playlistId, songId) },
                                    onRemoveFromLibrary = { viewModel.removeSongFromLibrary(it) }
                                )
                            }

                            composable<AppRoute.AlbumDetail> { backStackEntry ->
                                val route = backStackEntry.toRoute<AppRoute.AlbumDetail>()
                                val albumSongs = allSongs.filter { it.album == route.albumName }
                                CollectionDetailScreen(
                                    title = route.albumName,
                                    songs = albumSongs,
                                    playlists = playlists,
                                    playbackState = playbackState,
                                    onBack = { navController.popBackStack() },
                                    onPlaySongs = { list, idx -> viewModel.playSongs(list, idx) },
                                    onShuffleSongs = { list -> viewModel.shuffleAndPlay(list) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onPlayNext = { viewModel.playNext(it) },
                                    onAddToQueue = { viewModel.addToQueue(it) },
                                    onAddToPlaylist = { pid, ids -> viewModel.addSongsToPlaylist(pid, ids) },
                                    onCreatePlaylist = { name, ids -> viewModel.createPlaylist(name, ids) },
                                    onRemoveFromLibrary = { viewModel.removeSongFromLibrary(it) }
                                )
                            }

                            composable<AppRoute.ArtistDetail> { backStackEntry ->
                                val route = backStackEntry.toRoute<AppRoute.ArtistDetail>()
                                val artistSongs = allSongs.filter { it.artist == route.artistName }
                                CollectionDetailScreen(
                                    title = route.artistName,
                                    songs = artistSongs,
                                    playlists = playlists,
                                    playbackState = playbackState,
                                    onBack = { navController.popBackStack() },
                                    onPlaySongs = { list, idx -> viewModel.playSongs(list, idx) },
                                    onShuffleSongs = { list -> viewModel.shuffleAndPlay(list) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onPlayNext = { viewModel.playNext(it) },
                                    onAddToQueue = { viewModel.addToQueue(it) },
                                    onAddToPlaylist = { pid, ids -> viewModel.addSongsToPlaylist(pid, ids) },
                                    onCreatePlaylist = { name, ids -> viewModel.createPlaylist(name, ids) },
                                    onRemoveFromLibrary = { viewModel.removeSongFromLibrary(it) }
                                )
                            }

                            composable<AppRoute.FolderDetail> { backStackEntry ->
                                val route = backStackEntry.toRoute<AppRoute.FolderDetail>()
                                val folderSongs = allSongs.filter { it.folderPath == route.folderPath }
                                CollectionDetailScreen(
                                    title = route.folderName,
                                    songs = folderSongs,
                                    playlists = playlists,
                                    playbackState = playbackState,
                                    canExcludeFolder = true,
                                    onBack = { navController.popBackStack() },
                                    onPlaySongs = { list, idx -> viewModel.playSongs(list, idx) },
                                    onShuffleSongs = { list -> viewModel.shuffleAndPlay(list) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onPlayNext = { viewModel.playNext(it) },
                                    onAddToQueue = { viewModel.addToQueue(it) },
                                    onAddToPlaylist = { pid, ids -> viewModel.addSongsToPlaylist(pid, ids) },
                                    onCreatePlaylist = { name, ids -> viewModel.createPlaylist(name, ids) },
                                    onExcludeFolder = { viewModel.excludeFolder(route.folderPath, route.folderName) },
                                    onRemoveFromLibrary = { viewModel.removeSongFromLibrary(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
