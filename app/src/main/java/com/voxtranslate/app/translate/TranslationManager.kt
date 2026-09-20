package com.voxtranslate.app.translate

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

sealed class TranslateResult {
    data class Success(val text: String) : TranslateResult()
    data class NeedsDownload(val message: String, val retry: suspend () -> TranslateResult) : TranslateResult()
    data class Error(val message: String) : TranslateResult()
}

/**
 * Thin wrapper around ML Kit's on-device Translate API. Models download once
 * per language pair (Wi-Fi by default) and then translation works fully
 * offline — unlike the original desktop app's free web-scraping backends,
 * this never hits a rate limit and needs no internet after first download.
 */
object TranslationManager {

    private val cache = HashMap<String, Translator>()

    private fun clientFor(sourceCode: String, targetCode: String): Translator? {
        val source = TranslateLanguage.fromLanguageTag(sourceCode) ?: return null
        val target = TranslateLanguage.fromLanguageTag(targetCode) ?: return null
        val key = "$source-$target"
        return cache.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(target)
                .build()
            Translation.getClient(options)
        }
    }

    suspend fun translate(
        text: String,
        sourceCode: String,
        targetCode: String,
        allowDownloadOnAnyNetwork: Boolean = true
    ): TranslateResult {
        if (text.isBlank()) return TranslateResult.Success("")
        if (sourceCode == targetCode) return TranslateResult.Success(text)

        return try {
            val client = clientFor(sourceCode, targetCode)
                ?: return TranslateResult.Error("This language isn't supported for on-device translation yet.")

            // Downloads the language model on first use for this pair (a few MB), then caches
            // it on-device. Allowed on any network by default so translation "just works" the
            // first time; pass allowDownloadOnAnyNetwork = false to restrict downloads to Wi-Fi.
            val conditions = DownloadConditions.Builder().apply {
                if (!allowDownloadOnAnyNetwork) requireWifi()
            }.build()
            client.downloadModelIfNeeded(conditions).await()
            val result = client.translate(text).await()
            TranslateResult.Success(result)
        } catch (e: Exception) {
            TranslateResult.Error(
                "Translation failed: ${e.message ?: e.javaClass.simpleName}. " +
                    "Make sure you're connected to the internet the first time you use a new language pair " +
                    "(models download once, then work offline)."
            )
        }
    }

    fun close() {
        cache.values.forEach { it.close() }
        cache.clear()
    }
}
