package ru.kuzdikenov.fooddiary.service

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
        return redisTemplate.opsForValue().get(key(query))?.copy(cached = true)
    }


    fun put(query: String, product: ProductLookup) {
        redisTemplate.opsForValue().set(key(query), product, Duration.ofDays(appProperties.nutritionLookupCacheDurationDays))
    }

    private fun key(query: String): String {
        return "nutrition-lookup:${query.trim().lowercase()}"
    }

}
