package ru.kuzdikenov.fooddiary.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import ru.kuzdikenov.fooddiary.dto.ProductLookup
import tools.jackson.databind.ObjectMapper

@Configuration
class RedisConfig {

    @Bean
    fun productLookupRedisTemplate(
        connectionFactory: RedisConnectionFactory,
        objectMapper: ObjectMapper,
    ): RedisTemplate<String, ProductLookup> {
        val valueSerializer = JacksonJsonRedisSerializer(objectMapper, ProductLookup::class.java)
        val keySerializer = StringRedisSerializer()

        return RedisTemplate<String, ProductLookup>().apply {
            setConnectionFactory(connectionFactory)
            this.keySerializer = keySerializer
            this.valueSerializer = valueSerializer
            hashKeySerializer = keySerializer
            hashValueSerializer = valueSerializer
            afterPropertiesSet()
        }
    }
}
