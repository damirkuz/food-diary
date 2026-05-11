package ru.kuzdikenov.fooddiary.exception

class ProductSortException(sort: String) : RuntimeException(
    "Недопустимое значение sort: $sort"
)
