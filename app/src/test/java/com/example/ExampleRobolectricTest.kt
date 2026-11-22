package com.example

import android.content.Context
import android.net.Uri
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.KarlukDatabase
import com.example.data.database.SongEntity
import com.example.data.datastore.UserPreferencesRepository
import com.example.data.media.MediaScanner
import com.example.data.repository.MusicRepository
import com.example.domain.model.AppLanguage
import com.example.localization.LocaleHelper
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Karluk MP3 Pro`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Karluk MP3 Pro", appName)
    }

    @Test
    fun `localization resolves all four languages and RTL directions`() {
        val base = ApplicationProvider.getApplicationContext<Context>()

        val ugCtx = LocaleHelper.wrapContext(base, AppLanguage.UYGHUR)
        assertEquals("باش بەت", ugCtx.getString(R.string.nav_home))
        assertEquals("نەشىدلەر", ugCtx.getString(R.string.nav_library))
        assertEquals("كېچىلىك ھالەت", ugCtx.getString(R.string.theme_night_mode))
        assertEquals("كۈندۈزلۈك ھالەت", ugCtx.getString(R.string.theme_day_mode))
        assertEquals("ئۆچۈرۈش", ugCtx.getString(R.string.action_delete))
        assertEquals("نەشىد تەگلىكى", ugCtx.getString(R.string.now_playing_custom_bg))
        assertTrue(ugCtx.getString(R.string.theme_custom_bg_title).isNotBlank())
        assertEquals(LayoutDirection.Rtl, LocaleHelper.layoutDirectionFor(AppLanguage.UYGHUR))

        val arCtx = LocaleHelper.wrapContext(base, AppLanguage.ARABIC)
        assertEquals("الرئيسية", arCtx.getString(R.string.nav_home))
        assertEquals(LayoutDirection.Rtl, LocaleHelper.layoutDirectionFor(AppLanguage.ARABIC))

        val trCtx = LocaleHelper.wrapContext(base, AppLanguage.TURKISH)
        assertEquals("Ana Sayfa", trCtx.getString(R.string.nav_home))
        assertEquals(LayoutDirection.Ltr, LocaleHelper.layoutDirectionFor(AppLanguage.TURKISH))

        val enCtx = LocaleHelper.wrapContext(base, AppLanguage.ENGLISH)
        assertEquals("Home", enCtx.getString(R.string.nav_home))
        assertEquals(LayoutDirection.Ltr, LocaleHelper.layoutDirectionFor(AppLanguage.ENGLISH))
        assertTrue(ugCtx.getString(R.string.nav_favorites).isNotBlank())
    }

    @Test
    fun `onboarding completed state persists synchronously across reads`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferencesRepository(context)

        assertFalse(prefs.getOnboardingCompletedSync())
        prefs.setOnboardingCompletedSync(true)
        assertTrue(prefs.getOnboardingCompletedSync())
        assertTrue(prefs.getInitialSettingsSync().onboardingCompleted)
    }

    @Test
    fun `custom gallery background copies to internal storage and clears cleanly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferencesRepository(context)

        val tempSource = File(context.cacheDir, "sample_gallery_bg.jpg").apply {
            writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
        }
        val savedUri = prefs.saveCustomBackgroundFromUri(Uri.fromFile(tempSource))
        assertNotNull(savedUri)
        assertEquals(savedUri, prefs.getInitialSettingsSync().customBackgroundUri)

        prefs.clearCustomBackground()
        assertNull(prefs.getInitialSettingsSync().customBackgroundUri)
    }

    @Test
    fun `per-Nasheed background saves independently and clears without affecting other tracks`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = KarlukDatabase.getInstance(context)
        val dao = db.karlukDao()
        val repo = MusicRepository(context, dao, MediaScanner(context))

        dao.upsertSongs(
            listOf(
                SongEntity(
                    id = 101L, title = "Nasheed 1", artist = "Reciter", album = "Album",
                    duration = 120_000L, uri = "content://test/101", mimeType = "audio/mpeg",
                    formatLabel = "MP3", artworkUri = null, fileSize = 1024L,
                    dateAdded = 100L, trackNumber = 1, genre = null,
                    folderPath = "/music", folderName = "Music", isSafTrack = false
                ),
                SongEntity(
                    id = 102L, title = "Nasheed 2", artist = "Reciter", album = "Album",
                    duration = 140_000L, uri = "content://test/102", mimeType = "audio/mpeg",
                    formatLabel = "MP3", artworkUri = null, fileSize = 1024L,
                    dateAdded = 101L, trackNumber = 2, genre = null,
                    folderPath = "/music", folderName = "Music", isSafTrack = false
                )
            )
        )

        val img1 = File(context.cacheDir, "nasheed_101_bg.jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        val saved1 = repo.saveSongBackgroundFromUri(101L, Uri.fromFile(img1))
        assertNotNull(saved1)

        val songsAfterSave = repo.allSongsFlow.first()
        assertEquals(saved1, songsAfterSave.first { it.id == 101L }.customBackgroundUri)
        assertNull(songsAfterSave.first { it.id == 102L }.customBackgroundUri)

        repo.clearSongBackground(101L)
        val songsAfterClear = repo.allSongsFlow.first()
        assertNull(songsAfterClear.first { it.id == 101L }.customBackgroundUri)

        repo.renameSong(101L, "تاللانغان نەشىد")
        val songsAfterRename = repo.allSongsFlow.first()
        assertEquals("تاللانغان نەشىد", songsAfterRename.first { it.id == 101L }.title)
        assertEquals("تاللانغان نەشىد", repo.getSongById(101L)?.title)
    }

    @Test
    fun `startup intro video raw resource and new action strings exist`() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val ugCtx = LocaleHelper.wrapContext(base, AppLanguage.UYGHUR)
        assertEquals("ھەمبەھىرلەش", ugCtx.getString(R.string.action_share))
        assertEquals("قويۇش", ugCtx.getString(R.string.action_play))
        assertEquals("نەشىد نامىنى ئۆزگەرتىش", ugCtx.getString(R.string.rename_nasheed_title))
        assertEquals("كۆرسىتىش ۋە سۈزۈش", ugCtx.getString(R.string.library_menu_title))

        base.resources.openRawResource(R.raw.karluk_intro).use { stream ->
            assertTrue(stream.read() != -1)
        }
    }
}
