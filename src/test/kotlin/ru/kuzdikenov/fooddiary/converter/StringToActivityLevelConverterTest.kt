package ru.kuzdikenov.fooddiary.converter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import ru.kuzdikenov.fooddiary.entity.ActivityLevel

class StringToActivityLevelConverterTest {

    private val converter = StringToActivityLevelConverter()

    @Test
    fun `converts activity level ignoring case and spaces`() {
        assertEquals(ActivityLevel.MODERATE, converter.convert(" moderate "))
    }

    @Test
    fun `rejects unknown activity level`() {
        assertThrows(IllegalArgumentException::class.java) {
            converter.convert("weekly")
        }
    }
}
