package com.elementary.tasks.core.utils

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.elementary.tasks.R
import java.util.*

class Language(private val prefs: Prefs){

    /**
     * Holder locale for tts.
     *
     * @param isBirth flag for birthdays.
     * @return Locale
     */
    fun getLocale(isBirth: Boolean): Locale? {
        var res: Locale? = null
        when ((if (isBirth) {
            prefs.birthdayTtsLocale
        } else {
            prefs.ttsLocale
        })) {
            ENGLISH -> res = Locale.ENGLISH
            FRENCH -> res = Locale.FRENCH
            GERMAN -> res = Locale.GERMAN
            JAPANESE -> res = Locale.JAPANESE
            ITALIAN -> res = Locale.ITALIAN
            KOREAN -> res = Locale.KOREAN
            POLISH -> res = Locale("pl", "")
            RUSSIAN -> res = Locale("ru", "")
            SPANISH -> res = Locale("es", "")
            UKRAINIAN -> res = Locale("uk", "")
            PORTUGUESE -> res = Locale("pt", "")
        }
        return res
    }

    fun onAttach(context: Context): Context {
        return setLocale(context, getScreenLanguage(prefs.appLanguage))
    }

    private fun setLocale(context: Context, locale: Locale): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResources(context, locale)
        } else updateResourcesLegacy(context, locale)
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun updateResources(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }

    @Suppress("DEPRECATION")
    private fun updateResourcesLegacy(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = resources.configuration
        configuration.locale = locale
        try {
            configuration.setLayoutDirection(locale)
            resources.updateConfiguration(configuration, resources.displayMetrics)
        } catch (e: NoSuchMethodError) {
        }
        return context
    }

    fun getLocalized(context: Context, id: Int): String {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale(getTextLanguage(prefs.voiceLocale)))
        return context.createConfigurationContext(configuration).resources.getString(id)
    }

    fun getLanguages(context: Context): List<String> {
        val locales = ArrayList<String>()
        locales.add(context.getString(R.string.english) + " (" + EN + ")")
        locales.add(context.getString(R.string.russian) + " (" + RU + ")")
        locales.add(context.getString(R.string.ukrainian) + " (" + UK + ")")
        locales.add(context.getString(R.string.german) + " (" + DE + ") (BETA)")
        locales.add(context.getString(R.string.spanish) + " (" + ES + ") (BETA)")
        locales.add(context.getString(R.string.portuguese) + " (" + PT + ") (BETA)")
        return locales
    }

    fun getTextLanguage(code: Int): String {
        return when (code) {
            0 -> ENGLISH
            1 -> RUSSIAN
            2 -> UKRAINIAN
            3 -> GERMAN
            4 -> SPANISH
            5 -> PORTUGUESE
            else -> ENGLISH
        }
    }

    fun getLanguage(code: Int): String {
        return when (code) {
            0 -> EN
            1 -> RU
            2 -> UK
            3 -> DE
            4 -> ES
            5 -> PT
            else -> EN
        }
    }

    fun getVoiceLocale(code: Int): Locale {
        return when (code) {
            0 -> Locale.ENGLISH
            1 -> Locale("ru", "")
            2 -> Locale("uk", "")
            3 -> Locale.GERMAN
            4 -> Locale("es", "")
            5 -> Locale("pt", "")
            else -> Locale.ENGLISH
        }
    }

    fun getVoiceLanguage(code: Int): String {
        return when (code) {
            0 -> com.backdoor.engine.misc.Locale.EN
            1 -> com.backdoor.engine.misc.Locale.RU
            2 -> com.backdoor.engine.misc.Locale.UK
            3 -> com.backdoor.engine.misc.Locale.DE
            4 -> com.backdoor.engine.misc.Locale.ES
            5 -> com.backdoor.engine.misc.Locale.PT
            else -> com.backdoor.engine.misc.Locale.EN
        }
    }

    fun getLocaleByPosition(position: Int): String {
        var locale = ENGLISH
        when (position) {
            0 -> locale = ENGLISH
            1 -> locale = FRENCH
            2 -> locale = GERMAN
            3 -> locale = ITALIAN
            4 -> locale = JAPANESE
            5 -> locale = KOREAN
            6 -> locale = POLISH
            7 -> locale = RUSSIAN
            8 -> locale = SPANISH
            9 -> locale = PORTUGUESE
            10 -> locale = UKRAINIAN
        }
        return locale
    }

    fun getLocalePosition(locale: String?): Int {
        if (locale == null) {
            return 0
        }
        var mItemSelect = 0
        when {
            locale.matches(ENGLISH.toRegex()) -> mItemSelect = 0
            locale.matches(FRENCH.toRegex()) -> mItemSelect = 1
            locale.matches(GERMAN.toRegex()) -> mItemSelect = 2
            locale.matches(ITALIAN.toRegex()) -> mItemSelect = 3
            locale.matches(JAPANESE.toRegex()) -> mItemSelect = 4
            locale.matches(KOREAN.toRegex()) -> mItemSelect = 5
            locale.matches(POLISH.toRegex()) -> mItemSelect = 6
            locale.matches(RUSSIAN.toRegex()) -> mItemSelect = 7
            locale.matches(SPANISH.toRegex()) -> mItemSelect = 8
            locale.matches(PORTUGUESE.toRegex()) -> mItemSelect = 9
            locale.matches(UKRAINIAN.toRegex()) -> mItemSelect = 10
        }
        return mItemSelect
    }

    fun getScreenLocaleName(context: Context): String {
        return context.resources.getStringArray(R.array.app_languages)[prefs.appLanguage]
    }

    fun getLocaleNames(context: Context?): List<String> {
        if (context == null) return emptyList()
        val names = ArrayList<String>()
        names.add(context.getString(R.string.english))
        names.add(context.getString(R.string.french))
        names.add(context.getString(R.string.german))
        names.add(context.getString(R.string.italian))
        names.add(context.getString(R.string.japanese))
        names.add(context.getString(R.string.korean))
        names.add(context.getString(R.string.polish))
        names.add(context.getString(R.string.russian))
        names.add(context.getString(R.string.spanish))
        names.add(context.getString(R.string.portuguese))
        names.add(context.getString(R.string.ukrainian))
        return names
    }

    companion object {
        const val ENGLISH = "en"
        const val FRENCH = "fr"
        const val GERMAN = "de"
        const val ITALIAN = "it"
        const val JAPANESE = "ja"
        const val KOREAN = "ko"
        const val POLISH = "pl"
        const val RUSSIAN = "ru"
        const val SPANISH = "es"
        const val UKRAINIAN = "uk"
        const val PORTUGUESE = "pt"

        private const val EN = "en-US"
        private const val RU = "ru-RU"
        private const val UK = "uk-UA"
        private const val DE = "de-DE"
        private const val ES = "es-ES"
        private const val PT = "pt-PT"

        fun getScreenLanguage(code: Int): Locale {
            when (code) {
                0 -> return Locale.getDefault()
                1 -> return Locale.ENGLISH
                2 -> return Locale.GERMAN
                3 -> return Locale("es", "")
                4 -> return Locale.FRENCH
                5 -> return Locale.ITALIAN
                6 -> return Locale("pt", "")
                7 -> return Locale("pl", "")
                8 -> return Locale("cs", "")
                9 -> return Locale("ro", "")
                10 -> return Locale("tr", "")
                11 -> return Locale("uk", "")
                12 -> return Locale("ru", "")
                13 -> return Locale.JAPANESE
                14 -> return Locale.CHINESE
                15 -> return Locale("hi", "")
                16 -> return Locale.KOREAN
                else -> return Locale.getDefault()
            }
        }
    }
}
