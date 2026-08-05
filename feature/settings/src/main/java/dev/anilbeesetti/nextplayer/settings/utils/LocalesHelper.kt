package dev.anilbeesetti.nextplayer.settings.utils

import java.util.Locale

object LocalesHelper {

    private val supportedAppLanguageTags = listOf(
        "ar", "bg", "bn", "bs", "ca", "cs", "da", "de", "el", "es", "et", "fa", "fi", "fr",
        "he", "hi", "hu", "ia", "id", "it", "ja", "kn", "ko", "lv", "ml", "ms", "my", "nb-NO",
        "nl", "pa", "pl", "pt", "pt-BR", "ro", "ru", "sq", "sv", "ta", "te", "th", "tr", "uk",
        "ur", "vi", "zh-CN", "zh-TW",
    )

    fun getSupportedAppLocales(): List<Pair<String, String>> = supportedAppLanguageTags.map { tag ->
        val locale = Locale.forLanguageTag(tag)
        locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) } to tag
    }.sortedBy { it.first }

    fun getAppLocaleDisplayName(languageTag: String): String {
        val locale = Locale.forLanguageTag(languageTag)
        return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
    }

    fun getAvailableLocales(): List<Pair<String, String>> {
        return try {
            Locale.getAvailableLocales().map {
                val key = it.isO3Language
                val language = it.displayLanguage
                Pair(language, key)
            }.distinctBy { it.second }.sortedBy { it.first }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf()
        }
    }

    fun getLocaleDisplayLanguage(key: String): String {
        return try {
            Locale.getAvailableLocales().first { it.isO3Language == key }.displayLanguage
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
