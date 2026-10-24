package ru.kuzdikenov.fooddiary.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Component
class BusinessMetrics(registry: MeterRegistry) {
    private val registrations = Counter.builder("food.diary.registrations")
        .description("Successfully committed user registrations")
        .register(registry)
    private val products = Counter.builder("food.diary.products.creations")
        .description("Successfully committed product creations")
        .register(registry)
    private val entries = Counter.builder("food.diary.entries.creations")
        .description("Successfully committed food diary entry creations")
        .register(registry)

    // Регистрируем ограниченный набор серий заранее, включая нулевые значения.
    private val lookups = listOf(
        "success" to false, "success" to true, "not_found" to false,
        "external_error" to false, "error" to false,
    ).associateWith { (outcome, cached) ->
        Counter.builder("food.diary.nutrition.lookups")
            .description("Completed nutrition lookup requests, including cache hits and failures")
            .tags("outcome", outcome, "cached", cached.toString())
            .register(registry)
    }

    fun registrationSucceeded() = incrementAfterCommit(registrations)

    fun productCreated() = incrementAfterCommit(products)

    fun foodEntryCreated() = incrementAfterCommit(entries)

    fun nutritionLookupCompleted(outcome: String, cached: Boolean) {
        lookups.getValue(outcome to cached).increment()
    }

    private fun incrementAfterCommit(counter: Counter) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() = counter.increment()
            })
        } else {
            counter.increment()
        }
    }
}
