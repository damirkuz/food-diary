package ru.kuzdikenov.fooddiary.service.nutrition

import ru.kuzdikenov.fooddiary.dto.ProductLookup

interface NutritionProvider {
    fun lookup(name: String): ProductLookup?
}
