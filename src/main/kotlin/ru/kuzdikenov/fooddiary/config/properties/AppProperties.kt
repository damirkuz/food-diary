package ru.kuzdikenov.fooddiary.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
class AppProperties (
    val nutritionLookupCacheDurationDays: Long
) {
}