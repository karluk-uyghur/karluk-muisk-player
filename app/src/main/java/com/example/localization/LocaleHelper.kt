package com.example.localization

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.unit.LayoutDirection
import com.example.domain.model.AppLanguage
import java.util.Locale

object LocaleHelper {

    fun wrapContext(base: Context, language: AppLanguage): Context {
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    fun layoutDirectionFor(language: AppLanguage): LayoutDirection {
        return if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    }
}
