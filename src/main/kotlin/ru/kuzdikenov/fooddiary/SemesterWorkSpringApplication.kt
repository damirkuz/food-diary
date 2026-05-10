package ru.kuzdikenov.fooddiary

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@EnableConfigurationProperties
@ConfigurationPropertiesScan
@SpringBootApplication
class SemesterWorkSpringApplication

fun main(args: Array<String>) {
    runApplication<SemesterWorkSpringApplication>(*args)
}
