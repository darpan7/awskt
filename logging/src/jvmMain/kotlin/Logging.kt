package com.steamstreet.awskt.logging

import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer
import net.logstash.logback.marker.Markers
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

/**
 * Create a suspendable context for logging with MDC.
 */
public suspend inline fun <T> mdcContext(vararg pairs: Pair<String, Any?>, crossinline block: suspend () -> T): T {
    val notNull = pairs.mapNotNull {
        if (it.second == null) null
        else it.first to it.second!!.toString()
    }
    return withContext(MDCContext(*notNull.toTypedArray())) {
        block()
    }
}

/**
 * Construct an MDC context with data.
 */
public fun MDCContext(vararg pairs: Pair<String, String>): MDCContext =
    MDCContext(MDC.getCopyOfContextMap().orEmpty() + pairs)


public fun <T> mdc(vararg metadata: Pair<String, Any?>, block: () -> T): T {
    val previous = hashMapOf<String, Any?>()
    metadata.forEach {
        previous[it.first] = MDC.get(it.first)
        MDC.put(it.first, it.second.toString())
    }
    val result = block()
    previous.forEach { (key, value) ->
        if (value == null) {
            MDC.remove(key)
        } else {
            MDC.put(key, value.toString())
        }
    }
    return result
}

private val defaultLogger = LoggerFactory.getLogger("EventLogger")

/**
 * Log a JSON structure to the log record.
 */
public fun Logger.logJson(message: String, key: String, json: String) {
    this.info(Markers.appendRaw(key, json), message)
}

/**
 * Log serialized JSON to the log.
 */
public fun <T> Logger.logJson(message: String, key: String, serializer: KSerializer<T>, data: T) {
    logJson(message, key, Json.encodeToString(serializer, data))
}

/**
 * Log a value as structured json, assigning it to a field.
 */
public inline fun <reified T> Logger.logValue(message: String, field: String, data: T) {
    val element = Json.encodeToString(data)
    logJson(message, field, element)
}

/**
 * Log serialized JSON to the log.
 */
public fun <T> logJson(message: String, key: String, serializer: KSerializer<T>, data: T) {
    defaultLogger.logJson(message, key, Json.encodeToString(serializer, data))
}

/**
 * Log serialized JSON to the log.
 */
public inline fun <reified T> logValue(message: String, field: String, data: T) {
    logJson(message, field, Json.serializersModule.serializer<T>(), data)
}
/**
 * Log a value as structured json, setting the data values at the root of the
 * log message.
 */
public inline fun <reified T> Logger.logValue(message: String, data: T) {
    val element = Json.encodeToJsonElement(data)
    if (element is JsonObject) {
        val markers = element.map { (key, value) ->
            Markers.appendRaw(key, value.toString())
        }
        info(Markers.aggregate(markers), message)
    } else {
        logJson(message, "data", element.toString())
    }
}

/**
 * Log warning with additional metadata
 */
public fun logWarning(message: String, vararg metadata: Pair<String, Any?>) {
    mdc(*metadata) {
        defaultLogger.warn(message)
    }
}

public fun logWarning(message: String, throwable: Throwable?, vararg metadata: Pair<String, Any?>) {
    mdc(*metadata) {
        defaultLogger.warn(message, throwable)
    }
}

/**
 * Log info with additional metadata
 */
public fun logInfo(message: String, vararg metadata: Pair<String, Any?>) {
    mdc(*metadata) {
        defaultLogger.info(message)
    }
}


/**
 * Log info with additional metadata
 */
public fun logInfo(message: String, builderAction: MutableList<Pair<String, Any>>.() -> Unit) {
    val metadata: List<Pair<String, Any>> = buildList(builderAction)
    mdc(*metadata.toTypedArray()) {
        defaultLogger.info(message)
    }
}