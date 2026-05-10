package ru.kuzdikenov.fooddiary.entity

import java.math.BigDecimal

enum class ActivityLevel(
    val coefficient: BigDecimal
) {
    SEDENTARY(BigDecimal("1.20")),
    LIGHT(BigDecimal("1.375")),
    MODERATE(BigDecimal("1.55")),
    HIGH(BigDecimal("1.725")),
    VERY_HIGH(BigDecimal("1.90"))
}