package com.voxtranslate.app.translate

/**
 * One entry per language VoxTranslate supports. `mlkitCode` is the BCP-47
 * tag passed to ML Kit's on-device Translate API (via
 * TranslateLanguage.fromLanguageTag); `speechLocale` is the tag used by
 * Android's SpeechRecognizer / TextToSpeech for that language. These mirror
 * the language list and codes from the original VoxTranslate desktop app.
 */
data class VoxLanguage(
    val displayName: String,
    val flag: String,
    val mlkitCode: String,
    val speechLocale: String
)

object Languages {
    val ALL: List<VoxLanguage> = listOf(
        VoxLanguage("English", "🇬🇧", "en", "en-US"),
        VoxLanguage("Telugu", "🇮🇳", "te", "te-IN"),
        VoxLanguage("Hindi", "🇮🇳", "hi", "hi-IN"),
        VoxLanguage("Tamil", "🇮🇳", "ta", "ta-IN"),
        VoxLanguage("Kannada", "🇮🇳", "kn", "kn-IN"),
        VoxLanguage("Malayalam", "🇮🇳", "ml", "ml-IN"),
        VoxLanguage("Bengali", "🇮🇳", "bn", "bn-IN"),
        VoxLanguage("Marathi", "🇮🇳", "mr", "mr-IN"),
        VoxLanguage("Gujarati", "🇮🇳", "gu", "gu-IN"),
        VoxLanguage("Punjabi", "🇮🇳", "pa", "pa-IN"),
        VoxLanguage("Urdu", "🇵🇰", "ur", "ur-IN"),
        VoxLanguage("Korean", "🇰🇷", "ko", "ko-KR"),
        VoxLanguage("Japanese", "🇯🇵", "ja", "ja-JP"),
        VoxLanguage("Chinese", "🇨🇳", "zh", "zh-CN"),
        VoxLanguage("French", "🇫🇷", "fr", "fr-FR"),
        VoxLanguage("German", "🇩🇪", "de", "de-DE"),
        VoxLanguage("Spanish", "🇪🇸", "es", "es-ES"),
        VoxLanguage("Russian", "🇷🇺", "ru", "ru-RU"),
        VoxLanguage("Arabic", "🇸🇦", "ar", "ar-SA"),
        VoxLanguage("Portuguese", "🇵🇹", "pt", "pt-PT"),
        VoxLanguage("Italian", "🇮🇹", "it", "it-IT"),
    )

    fun byName(name: String): VoxLanguage = ALL.first { it.displayName == name }

    val DEFAULT_SOURCE = byName("English")
    val DEFAULT_TARGET = byName("Hindi")
}
