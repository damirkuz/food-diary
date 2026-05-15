package ru.kuzdikenov.fooddiary.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.kuzdikenov.fooddiary.service.translate.TranslationProvider

class ProductSearchQueryServiceTest {

    @Test
    fun `uses translated query for cyrillic product name`() {
        val service = ProductSearchQueryService(
            translationProvider = object : TranslationProvider {
                override fun translate(text: String): String? = "apple"
            }
        )

        assertEquals(listOf("apple"), service.buildQueries(" яблоко "))
    }

    @Test
    fun `uses original query for latin product name`() {
        val service = ProductSearchQueryService(
            translationProvider = object : TranslationProvider {
                override fun translate(text: String): String? = error("Translation should not be called")
            }
        )

        assertEquals(listOf("banana"), service.buildQueries(" banana "))
    }
}
