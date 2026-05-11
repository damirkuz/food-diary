package ru.kuzdikenov.fooddiary.exception

class ProductUsedInFoodEntriesException : RuntimeException("Продукт используется в записях дневника и не может быть удалён")
