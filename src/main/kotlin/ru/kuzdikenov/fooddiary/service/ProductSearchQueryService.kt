package ru.kuzdikenov.fooddiary.service

import org.springframework.stereotype.Service
import ru.kuzdikenov.fooddiary.exception.ExternalTranslateException
import ru.kuzdikenov.fooddiary.service.translate.TranslationProvider

@Service
class ProductSearchQueryService(
    private val translationProvider: TranslationProvider,
) {

    fun buildQueries(text: String): List<String> {
        val normalizedText = text.trim()

        if (containsCyrillic(normalizedText)) {
            val translatedText = try {
                translationProvider.translate(normalizedText)
            } catch (ex: ExternalTranslateException) {
                null
            }

            if (!translatedText.isNullOrBlank() && !translatedText.equals(normalizedText, ignoreCase = true)) {
                return listOf(translatedText.trim())
            }
        }

        return listOf(normalizedText)
    }

    private fun containsCyrillic(text: String): Boolean {
        return text.any { it in 'а'..'я' || it in 'А'..'Я' || it == 'ё' || it == 'Ё' }
    }
}
