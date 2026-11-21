package com.example

import android.content.Context
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.AppLanguage
import com.example.localization.LocaleHelper
import org.junit.Assert.assertEquals
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
}
