package ru.kuzdikenov.fooddiary.logging

import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

object BusinessEventLogger {
    private val logger = LoggerFactory.getLogger(BusinessEventLogger::class.java)

    fun afterCommit(event: String, message: String, fields: Map<String, Any?>) {
        val log = { record(event, message, "success", fields) }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() = log()
            })
        } else {
            // Без транзакции операция уже завершена, например при прямом вызове в тесте.
            log()
        }
    }

    fun record(
        event: String,
        message: String,
        outcome: String,
        fields: Map<String, Any?>,
        level: Level = Level.INFO,
    ) {
        val builder = logger.atLevel(level)
            .addKeyValue("event", event)
            .addKeyValue("outcome", outcome)
        fields.forEach { (key, value) -> if (value != null) builder.addKeyValue(key, value) }
        builder.log(message)
    }
}
