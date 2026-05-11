package ru.kuzdikenov.fooddiary.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "provider.my-memory-translate")
data class MyMemoryTranslateProperties(
    val baseUrl: String,
)
