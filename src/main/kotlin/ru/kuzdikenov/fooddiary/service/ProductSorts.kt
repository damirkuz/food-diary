package ru.kuzdikenov.fooddiary.service

import org.springframework.data.domain.Sort
import ru.kuzdikenov.fooddiary.exception.ProductSortException

object ProductSorts {

    fun user(sort: String): Sort {
        return common(sort) ?: throw ProductSortException(sort)
    }

    fun admin(sort: String): Sort {
        return common(sort) ?: when (sort) {
            "owner,asc" -> Sort.by(Sort.Direction.ASC, "owner.email")
            "owner,desc" -> Sort.by(Sort.Direction.DESC, "owner.email")
            else -> throw ProductSortException(sort)
        }
    }

    private fun common(sort: String): Sort? {
        return when (sort) {
            "createdAt,desc" -> Sort.by(Sort.Direction.DESC, "createdAt")
            "createdAt,asc" -> Sort.by(Sort.Direction.ASC, "createdAt")
            "name,asc" -> Sort.by(Sort.Direction.ASC, "name")
            "name,desc" -> Sort.by(Sort.Direction.DESC, "name")
            else -> null
        }
    }
}
