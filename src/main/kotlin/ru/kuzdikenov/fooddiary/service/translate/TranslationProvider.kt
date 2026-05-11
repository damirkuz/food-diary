package ru.kuzdikenov.fooddiary.service.translate

interface TranslationProvider {
    fun translate(text: String): String?
}
