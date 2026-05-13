package ru.kuzdikenov.fooddiary.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = "provider.usda")
data class UsdaProperties(
    val baseUrl: String,
    val apiKey: String = "",
    val enabled: Boolean = true,
)
