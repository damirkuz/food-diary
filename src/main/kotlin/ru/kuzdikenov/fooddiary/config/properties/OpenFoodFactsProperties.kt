package ru.kuzdikenov.fooddiary.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "provider.open-food-facts")
data class OpenFoodFactsProperties(
    val baseUrl: String,
)
