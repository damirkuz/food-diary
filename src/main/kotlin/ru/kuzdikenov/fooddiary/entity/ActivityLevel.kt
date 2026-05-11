package ru.kuzdikenov.fooddiary.entity

enum class ActivityLevel(
    val coefficient: Double
) {
    SEDENTARY(1.20),
    LIGHT(1.375),
    MODERATE(1.55),
    HIGH(1.725),
    VERY_HIGH(1.90)
}
