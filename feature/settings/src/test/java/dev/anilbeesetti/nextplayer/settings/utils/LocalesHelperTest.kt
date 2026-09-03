package dev.anilbeesetti.nextplayer.settings.utils

import org.junit.Assert.assertTrue
import org.junit.Test

class LocalesHelperTest {

    @Test
    fun supportedAppLocalesIncludeEnglish() {
        assertTrue(
            LocalesHelper.getSupportedAppLocales().any { (displayName, languageTag) ->
                languageTag == "en" && displayName == "English"
            },
        )
    }
}
