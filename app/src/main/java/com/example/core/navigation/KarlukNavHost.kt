package com.example.core.navigation

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.core.designsystem.KarlukBackground
import com.example.core.designsystem.LocalKarlukTokens
import com.example.core.designsystem.MiniPlayerBar
import com.example.core.designsystem.localizedAlbumName
import com.example.core.designsystem.localizedArtistName
import com.example.domain.model.AppLanguage
import com.example.domain.model.ThemeAtmosphere
import com.example.feature.equalizer.EqualizerScreen
import com.example.feature.favorites.FavoritesScreen
import com.example.feature.files.AddFilesScreen
import com.example.feature.files.FilteredMusicScreen
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
import com.example.feature.settings.TrashScreen
import com.example.feature.splash.OnboardingScreen
import com.example.feature.splash.SplashScreen
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch

private data class BottomTabItem(
    val pageIndex: Int,
    val icon: ImageVector,
    val labelRes: Int,
    val testTag: String
)

private object LaunchRecreateState {
    var skipIntroOnce: Boolean = false
    var restoreSettingsTabOnce: Boolean = false
}

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
    val coroutineScope = rememberCoroutineScope()

    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val filteredMusicSongs by viewModel.filteredMusicSongs.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val historySongs by viewModel.historySongs.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val safFolders by viewModel.safFolders.collectAsStateWithLifecycle()
    val excludedFolders by viewModel.excludedFolders.collectAsStateWithLifecycle()
    val trashItems by viewModel.trashItems.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val audioEffectsState by viewModel.audioEffectsState.collectAsStateWithLifecycle()
    val spectrum by viewModel.spectrumFlow.collectAsStateWithLifecycle()
    val currentLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val updateCheckFailed by viewModel.updateCheckFailed.collectAsStateWithLifecycle()
    val pendingDeleteRequest by viewModel.pendingDeleteRequest.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

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
        val onOnboarding = navController.currentDestination?.hasRoute<AppRoute.Onboarding>() == true
        if (granted) {
            viewModel.rescanMedia()
            if (onOnboarding) {
                viewModel.setOnboardingCompleted(true)
                navController.navigate(AppRoute.MainPager) {
                    popUpTo(AppRoute.Onboarding) { inclusive = true }
                }
            }
        } else if (onOnboarding) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.permission_denied_desc))
            }
        }
    }

    val onboardingSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.addSafFolder(uri)
            viewModel.setOnboardingCompleted(true)
            navController.navigate(AppRoute.MainPager) {
                popUpTo(AppRoute.Onboarding) { inclusive = true }
            }
        }
    }

    val systemDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onSystemDeleteResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(pendingDeleteRequest) {
        val req = pendingDeleteRequest ?: return@LaunchedEffect
        runCatching {
            systemDeleteLauncher.launch(IntentSenderRequest.Builder(req.first).build())
        }.onFailure {
            viewModel.onSystemDeleteResult(false)
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

    val topLevelTabs = remember {
        listOf(
            BottomTabItem(0, Icons.Rounded.Home, R.string.nav_home, "nav_home"),
            BottomTabItem(1, Icons.Rounded.LibraryMusic, R.string.nav_library, "nav_library"),
            BottomTabItem(2, Icons.Rounded.Favorite, R.string.nav_favorites, "nav_favorites"),
            BottomTabItem(3, Icons.Rounded.CreateNewFolder, R.string.nav_add_files, "nav_add_files"),
            BottomTabItem(4, Icons.Rounded.Settings, R.string.nav_settings, "nav_settings")
        )
    }

    val initialPage = remember {
        if (LaunchRecreateState.restoreSettingsTabOnce) {
            LaunchRecreateState.restoreSettingsTabOnce = false
            4
        } else {
            0
        }
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { topLevelTabs.size })

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isTopLevelRoute = currentDestination?.hasRoute<AppRoute.MainPager>() == true
    val isHomeRoute = isTopLevelRoute && pagerState.currentPage == 0

    fun navigateTopLevelPage(pageIndex: Int) {
        val target = pageIndex.coerceIn(0, topLevelTabs.lastIndex)
        if (isTopLevelRoute) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(target)
            }
        } else {
            coroutineScope.launch {
                pagerState.scrollToPage(target)
                navController.navigate(AppRoute.MainPager) {
                    popUpTo<AppRoute.MainPager> { inclusive = false }
                    launchSingleTop = true
                }
            }
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
                            val selected = pagerState.currentPage == tab.pageIndex
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevelPage(tab.pageIndex) },
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
                                            val selected = pagerState.currentPage == tab.pageIndex
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { navigateTopLevelPage(tab.pageIndex) },
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
                                val skipOnce = remember {
                                    LaunchRecreateState.skipIntroOnce.also {
                                        LaunchRecreateState.skipIntroOnce = false
                                    }
                                }
                                SplashScreen(
                                    onboardingCompleted = userSettings.onboardingCompleted,
                                    skipIntroOnce = skipOnce,
                                    onNavigateToOnboarding = {
                                        navController.navigate(AppRoute.Onboarding) {
                                            popUpTo(AppRoute.Splash) { inclusive = true }
                                        }
                                    },
                                    onNavigateToHome = {
                                        navController.navigate(AppRoute.MainPager) {
                                            popUpTo(AppRoute.Splash) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable<AppRoute.Onboarding> {
                                OnboardingScreen(
                                    currentLanguage = userSettings.language,
                                    onSelectLanguage = { lang ->
                                        LaunchRecreateState.skipIntroOnce = true
                                        onLanguageChanged(lang)
                                    },
                                    onRequestAudioPermission = {
                                        if (hasAudioPermission) {
                                            viewModel.setOnboardingCompleted(true)
                                            viewModel.rescanMedia()
                                            navController.navigate(AppRoute.MainPager) {
                                                popUpTo(AppRoute.Onboarding) { inclusive = true }
                                            }
                                        } else {
                                            permissionLauncher.launch(permissionsToRequest)
                                        }
                                    },
                                    onPickSafFolder = {
                                        onboardingSafLauncher.launch(null)
                                    }
                                )
                            }

                            composable<AppRoute.MainPager> {
                                BackHandler(enabled = pagerState.currentPage != 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(0)
                                    }
                                }

                                HorizontalPager(
                                    state = pagerState,
                                    beyondViewportPageCount = 1,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("main_horizontal_pager")
                                ) { page ->
                                    when (page) {
                                        0 -> {
                                            val isNightMode = userSettings.themeAtmosphere != ThemeAtmosphere.SILVER_FROST
                                            HomeDashboardScreen(
                                                playbackState = playbackState,
                                                spectrum = spectrum,
                                                allSongsCount = allSongs.size,
                                                recentSongs = historySongs,
                                                onOpenNowPlaying = { navController.navigate(AppRoute.NowPlaying) },
                                                onOpenSearch = { navController.navigate(AppRoute.Search) },
                                                onOpenSettings = { navigateTopLevelPage(4) },
                                                onOpenLibrary = { navigateTopLevelPage(1) },
                                                onOpenFavorites = { navigateTopLevelPage(2) },
                                                onOpenEqualizer = { navController.navigate(AppRoute.Equalizer) },
                                                onOpenAddFiles = { navigateTopLevelPage(3) },
                                                onOpenHistory = { navController.navigate(AppRoute.History(selectToClear = false)) },
                                                isNightMode = isNightMode,
                                                onToggleDayNight = {
                                                    viewModel.setThemeAtmosphere(
                                                        if (isNightMode) ThemeAtmosphere.SILVER_FROST else ThemeAtmosphere.DEEP_NAVY
                                                    )
                                                },
                                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                                onSkipNext = { viewModel.skipToNext() },
                                                onSkipPrev = { viewModel.skipToPrevious() },
                                                onSeekTo = { viewModel.seekTo(it) },
                                                onToggleShuffle = { viewModel.toggleShuffle() },
                                                onCycleRepeat = { viewModel.cycleRepeatMode() },
                                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                                onChangeSpeed = { viewModel.setPlaybackSpeed(it) },
                                                onPlaySong = { list, idx -> viewModel.playSongs(list, idx) },
                                                onRenameSong = { id, newTitle -> viewModel.renameSong(id, newTitle) },
                                                onDeleteHeroSong = { viewModel.removeSongFromLibrary(it) },
                                                onDeleteRecentSong = { viewModel.removeSongFromHistory(it) },
                                                onClearRecentHistory = { viewModel.clearHistory() },
                                                onMoveRecentSong = { id, delta -> viewModel.moveRecentSong(id, delta) }
                                            )
                                        }

                                        1 -> {
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
                                                onOpenAddFiles = { navigateTopLevelPage(3) },
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
                                                onRenameSong = { id, newTitle -> viewModel.renameSong(id, newTitle) },
                                                onRemoveFromLibrary = { viewModel.removeSongFromLibrary(it) },
                                                onBatchRemoveFromLibrary = { viewModel.removeSongsFromLibrary(it) },
                                                filteredMusicCount = filteredMusicSongs.size,
                                                onOpenFilteredMusic = { navController.navigate(AppRoute.FilteredMusic) }
                                            )
                                        }

                                        2 -> {
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
                                                onRenameSong = { id, newTitle -> viewModel.renameSong(id, newTitle) },
                                                onRemoveFromLibrary = { viewModel.removeSongFromLibrary(it) }
                                            )
                                        }

                                        3 -> {
                                            val importedSongs = remember(allSongs) {
                                                allSongs.filter {
                                                    it.folderPath == "saf_imports" ||
                                                        (it.isSafTrack && it.folderPath == "external")
                                                }
                                            }
                                            AddFilesScreen(
                                                safFolders = safFolders,
                                                excludedFolders = excludedFolders,
                                                deviceFolders = folders,
                                                importedSongs = importedSongs,
                                                storageStats = storageStats,
                                                isScanning = isScanning,
                                                hasAudioPermission = hasAudioPermission,
                                                onRequestPermission = { permissionLauncher.launch(permissionsToRequest) },
                                                onRescanMedia = { viewModel.rescanMedia() },
                                                onImportAudioFiles = { uris -> viewModel.importAudioFiles(uris, playImmediately = true) },
                                                onAddSafFolder = { uri -> viewModel.addSafFolder(uri) },
                                                onRemoveSafFolder = { viewModel.removeSafFolder(it) },
                                                onRemoveAllSafFolders = { viewModel.removeAllSafFolders() },
                                                onExcludeFolder = { path, name -> viewModel.excludeFolder(path, name) },
                                                onRestoreExcludedFolder = { viewModel.restoreFolder(it) },
                                                onPlaySong = { list, idx -> viewModel.playSongs(list, idx) },
                                                onRemoveImportedSong = { viewModel.removeSongFromLibrary(it) },
                                                onRemoveAllImportedSongs = { viewModel.removeSongsFromLibrary(it) },
                                                onOpenFolderDetail = { f -> navController.navigate(AppRoute.FolderDetail(f.path, f.name)) },
                                                onOpenStorageInfo = { navController.navigate(AppRoute.StorageInfo) },
                                                filteredMusicCount = filteredMusicSongs.size,
                                                onOpenFilteredMusic = { navController.navigate(AppRoute.FilteredMusic) }
                                            )
                                        }

                                        else -> {
                                            SettingsScreen(
                                                settings = userSettings,
                                                playbackState = playbackState,
                                                trashCount = trashItems.size,
                                                isCheckingUpdate = isCheckingUpdate,
                                                onSelectLanguage = { lang ->
                                                    LaunchRecreateState.skipIntroOnce = true
                                                    LaunchRecreateState.restoreSettingsTabOnce = true
                                                    onLanguageChanged(lang)
                                                },
                                                onToggleDayNight = { night ->
                                                    viewModel.setThemeAtmosphere(
                                                        if (night) ThemeAtmosphere.DEEP_NAVY else ThemeAtmosphere.SILVER_FROST
                                                    )
                                                },
                                                onOpenThemeStudio = { navController.navigate(AppRoute.ThemeStudio) },
                                                onSelectTextColor = { viewModel.setTextColorOption(it) },
                                                onSelectCustomTextColor = { viewModel.setCustomTextColorArgb(it) },
                                                onPickCustomBackground = { uri -> viewModel.setCustomBackground(uri) },
                                                onClearCustomBackground = { viewModel.clearCustomBackground() },
                                                onOpenEqualizer = { navController.navigate(AppRoute.Equalizer) },
                                                onOpenSleepTimer = { navController.navigate(AppRoute.SleepTimer) },
                                                onOpenStorageInfo = { navController.navigate(AppRoute.StorageInfo) },
                                                onOpenTrash = { navController.navigate(AppRoute.Trash) },
                                                onCheckUpdates = { viewModel.openUpdateDialogAndCheck() },
                                                onOpenAbout = { navController.navigate(AppRoute.About) },
                                                onToggleResumePlayback = { viewModel.setResumePlayback(it) },
                                                onToggleGapless = { viewModel.setGaplessPlayback(it) },
                                                onToggleAutoScan = { viewModel.setAutoScanOnStartup(it) },
                                                onClearHistory = { navController.navigate(AppRoute.History(selectToClear = true)) }
                                            )
                                        }
                                    }
                                }
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
                                    onOpenSleepTimer = { navController.navigate(AppRoute.SleepTimer) },
                                    onPickSongBackground = { songId, uri -> viewModel.setSongCustomBackground(songId, uri) },
                                    onClearSongBackground = { songId -> viewModel.clearSongCustomBackground(songId) },
                                    onRenameSong = { id, newTitle -> viewModel.renameSong(id, newTitle) },
                                    onDeleteSong = { viewModel.removeSongFromLibrary(it) }
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
                                    onRenameSong = { id, newTitle -> viewModel.renameSong(id, newTitle) },
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

                            composable<AppRoute.History> { backStackEntry ->
                                val route = backStackEntry.toRoute<AppRoute.History>()
                                HistoryScreen(
                                    historySongs = historySongs,
                                    playlists = playlists,
                                    playbackState = playbackState,
                                    initialSelectionMode = route.selectToClear,
                                    onBack = { navController.popBackStack() },
                                    onClearHistory = { viewModel.clearHistory() },
                                    onRemoveFromHistory = { viewModel.removeSongFromHistory(it) },
                                    onRemoveSongsFromHistory = { viewModel.removeSongsFromHistory(it) },
                                    onPlaySongs = { list, idx -> viewModel.playSongs(list, idx) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onPlayNext = { viewModel.playNext(it) },
                                    onAddToQueue = { viewModel.addToQueue(it) },
                                    onAddToPlaylist = { pid, ids -> viewModel.addSongsToPlaylist(pid, ids) },
                                    onCreatePlaylist = { name, ids -> viewModel.createPlaylist(name, ids) },
                                    onRenameSong = { id, newTitle -> viewModel.renameSong(id, newTitle) },
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
                                    onSelectTextColor = { viewModel.setTextColorOption(it) },
                                    onSelectCustomTextColor = { viewModel.setCustomTextColorArgb(it) },
                                    onChangeGlassOpacity = { viewModel.setGlassOpacity(it) },
                                    onToggleReducedMotion = { viewModel.setReducedMotion(it) },
                                    onToggleLowPower = { viewModel.setLowPowerMode(it) },
                                    onPickCustomBackground = { uri -> viewModel.setCustomBackground(uri) },
                                    onClearCustomBackground = { viewModel.clearCustomBackground() }
                                )
                            }

                            composable<AppRoute.StorageInfo> {
                                StorageInfoScreen(
                                    stats = storageStats,
                                    allSongs = allSongs,
                                    onBack = { navController.popBackStack() },
                                    onClearCache = { viewModel.clearArtworkCache() },
                                    onOpenLibrary = { navigateTopLevelPage(1) },
                                    onOpenAddFiles = { navigateTopLevelPage(3) },
                                    onPlaySongs = { list, idx -> viewModel.playSongs(list, idx) },
                                    onDeleteSong = { viewModel.removeSongFromLibrary(it) }
                                )
                            }

                            composable<AppRoute.Trash> {
                                TrashScreen(
                                    trashItems = trashItems,
                                    onBack = { navController.popBackStack() },
                                    onRestoreItem = { viewModel.restoreTrashItem(it) },
                                    onDeleteForeverItem = { viewModel.requestDeleteTrashItem(it) },
                                    onEmptyTrash = { viewModel.requestEmptyTrash() }
                                )
                            }

                            composable<AppRoute.FilteredMusic> {
                                FilteredMusicScreen(
                                    songs = filteredMusicSongs,
                                    playbackState = playbackState,
                                    onBack = { navController.popBackStack() },
                                    onPreviewSongs = { list, idx -> viewModel.playSongs(list, idx) },
                                    onApproveMoveToNasheeds = { ids -> viewModel.approveFilteredSongsAsNasheed(ids) },
                                    onRemoveFromLibrary = { viewModel.removeSongFromLibrary(it) }
                                )
                            }

                            composable<AppRoute.About> {
                                AboutScreen(
                                    onBack = { navController.popBackStack() },
                                    updateInfo = updateInfo,
                                    isCheckingUpdate = isCheckingUpdate,
                                    onCheckUpdates = { viewModel.openUpdateDialogAndCheck() }
                                )
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
                                    onRenameSong = { id, newTitle -> viewModel.renameSong(id, newTitle) },
                                    onRemoveFromPlaylist = { songId -> viewModel.removeSongFromPlaylist(route.playlistId, songId) },
                                    onRemoveFromLibrary = { viewModel.removeSongFromLibrary(it) }
                                )
                            }

                            composable<AppRoute.AlbumDetail> { backStackEntry ->
                                val route = backStackEntry.toRoute<AppRoute.AlbumDetail>()
                                val albumSongs = allSongs.filter { it.album == route.albumName }
                                CollectionDetailScreen(
                                    title = localizedAlbumName(route.albumName),
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
                                    onRenameSong = { id, newTitle -> viewModel.renameSong(id, newTitle) },
                                    onRemoveFromLibrary = { viewModel.removeSongFromLibrary(it) }
                                )
                            }

                            composable<AppRoute.ArtistDetail> { backStackEntry ->
                                val route = backStackEntry.toRoute<AppRoute.ArtistDetail>()
                                val artistSongs = allSongs.filter { it.artist == route.artistName }
                                CollectionDetailScreen(
                                    title = localizedArtistName(route.artistName),
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
                                    onRenameSong = { id, newTitle -> viewModel.renameSong(id, newTitle) },
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
                                    onRenameSong = { id, newTitle -> viewModel.renameSong(id, newTitle) },
                                    onExcludeFolder = { viewModel.excludeFolder(route.folderPath, route.folderName) },
                                    onRemoveFromLibrary = { viewModel.removeSongFromLibrary(it) }
                                )
                            }
                        }
                    }
                }
            }

            if (showUpdateDialog) {
                val info = updateInfo
                AlertDialog(
                    onDismissRequest = { viewModel.dismissUpdateBanner() },
                    containerColor = if (tokens.isLightTheme) Color.White else Color(0xFF0E2150),
                    title = {
                        Text(
                            text = stringResource(R.string.update_dialog_title),
                            color = tokens.textPrimary
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = stringResource(R.string.update_current_version_label, com.example.BuildConfig.VERSION_NAME),
                                style = MaterialTheme.typography.labelLarge,
                                color = tokens.accentPrimary
                            )
                            Text(
                                text = com.example.data.update.AppUpdateChecker.TELEGRAM_CHANNEL_URL,
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.textSecondary
                            )

                            when {
                                isCheckingUpdate -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.padding(2.dp),
                                            color = tokens.accentPrimary
                                        )
                                        Text(
                                            text = stringResource(R.string.update_checking_msg),
                                            color = tokens.textSecondary
                                        )
                                    }
                                }

                                updateCheckFailed -> {
                                    Text(
                                        text = stringResource(R.string.update_check_failed),
                                        color = Color(0xFFFF6E8A)
                                    )
                                }

                                info != null && info.isUpdateAvailable -> {
                                    Text(
                                        text = stringResource(R.string.update_latest_version_label, info.latestVersion),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = tokens.emerald
                                    )
                                    Text(
                                        text = stringResource(R.string.update_available_desc),
                                        color = tokens.textSecondary
                                    )
                                    if (info.releaseNotes.isNotBlank()) {
                                        Text(
                                            text = info.releaseNotes,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = tokens.textPrimary,
                                            maxLines = 5,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                info != null -> {
                                    Text(
                                        text = stringResource(R.string.update_up_to_date, com.example.BuildConfig.VERSION_NAME),
                                        color = tokens.emerald,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (info.releaseNotes.isNotBlank()) {
                                        Text(
                                            text = info.releaseNotes,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = tokens.textPrimary,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        runCatching {
                                            uriHandler.openUri(
                                                info?.downloadUrl ?: com.example.data.update.AppUpdateChecker.TELEGRAM_CHANNEL_URL
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = tokens.accentPrimary,
                                        contentColor = Color(0xFF031126)
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("update_dialog_download_btn")
                                ) {
                                    Text(stringResource(R.string.update_download_latest), fontWeight = FontWeight.Bold)
                                }

                                androidx.compose.material3.OutlinedButton(
                                    onClick = { viewModel.checkForUpdates(manual = true, openDialog = true) },
                                    enabled = !isCheckingUpdate,
                                    modifier = Modifier.fillMaxWidth().testTag("update_dialog_check_btn")
                                ) {
                                    Text(stringResource(R.string.update_check_version), color = tokens.textPrimary)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissUpdateBanner() }) {
                            Text(stringResource(R.string.action_cancel), color = tokens.textSecondary)
                        }
                    }
                )
            }
        }
    }
}
