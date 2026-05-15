package ru.kuzdikenov.fooddiary.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import ru.kuzdikenov.fooddiary.exception.ProductSortException

class ProductSortsTest {

    @Test
    fun `builds user sort`() {
        val order = ProductSorts.user("name,desc").first()

        assertEquals("name", order.property)
        assertEquals(Sort.Direction.DESC, order.direction)
    }

    @Test
    fun `builds admin owner sort`() {
        val order = ProductSorts.admin("owner,asc").first()

        assertEquals("owner.email", order.property)
        assertEquals(Sort.Direction.ASC, order.direction)
    }

    @Test
    fun `rejects unsupported sort`() {
        assertThrows(ProductSortException::class.java) {
            ProductSorts.user("unknown,asc")
        }
    }
}
