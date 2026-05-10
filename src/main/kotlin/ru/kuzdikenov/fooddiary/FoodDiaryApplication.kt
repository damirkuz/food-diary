package ru.kuzdikenov.fooddiary

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@EnableConfigurationProperties
@ConfigurationPropertiesScan
@SpringBootApplication
class FoodDiaryApplication

fun main(args: Array<String>) {
    runApplication<FoodDiaryApplication>(*args)
}
