package ru.kuzdikenov.fooddiary.converter

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import ru.kuzdikenov.fooddiary.entity.ActivityLevel

@Component
class StringToActivityLevelConverter : Converter<String, ActivityLevel> {

    override fun convert(source: String): ActivityLevel {
        return ActivityLevel.entries.firstOrNull {
            it.name.equals(source.trim(), ignoreCase = true)
        } ?: throw IllegalArgumentException("Unknown activity level: $source")
    }
}