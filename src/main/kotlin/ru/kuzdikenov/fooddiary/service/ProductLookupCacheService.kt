package ru.kuzdikenov.fooddiary.service

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import ru.kuzdikenov.fooddiary.config.properties.AppProperties
import ru.kuzdikenov.fooddiary.dto.ProductLookup
import java.time.Duration

@Service
class ProductLookupCacheService(
    private val redisTemplate: RedisTemplate<String, ProductLookup>,
    private val appProperties: AppProperties
) {

    fun get(query: String): ProductLookup? {
        return try {
            redisTemplate.opsForValue().get(key(query))?.copy(cached = true)
        } catch (ex: RuntimeException) {
            logger.warn("Nutrition lookup cache read failed for query '{}'", query, ex)
            null
        }
    }


    fun put(query: String, product: ProductLookup) {
        try {
            redisTemplate.opsForValue().set(
                key(query),
                product,
                Duration.ofDays(appProperties.nutritionLookupCacheDurationDays)
            )
        } catch (ex: RuntimeException) {
            logger.warn("Nutrition lookup cache write failed for query '{}'", query, ex)
        }
    }

    private fun key(query: String): String {
        return "nutrition-lookup:${query.trim().lowercase()}"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProductLookupCacheService::class.java)
    }
}
