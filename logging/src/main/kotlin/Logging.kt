package com.steamstreet.awskt.logging

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

public fun <T> mdc(vararg metadata: Pair<String, String?>, block: () -> T): T {
    val previous = hashMapOf<String, String?>()
    metadata.forEach {
        previous[it.first] = MDC.get(it.first)
        MDC.put(it.first, it.second)
    }
    val result = block()
    previous.forEach { (key, value) ->
        if (value == null) {
            MDC.remove(key)
        } else {
            MDC.put(key, value)
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
public fun logWarning(message: String, vararg metadata: Pair<String, String?>) {
    mdc(*metadata) {
        defaultLogger.warn(message)
    }
}

public fun logWarning(message: String, throwable: Throwable?, vararg metadata: Pair<String, String?>) {
    mdc(*metadata) {
        defaultLogger.warn(message, throwable)
    }
}

/**
 * Log info with additional metadata
 */
public fun logInfo(message: String, vararg metadata: Pair<String, String?>) {
    mdc(*metadata) {
        defaultLogger.info(message)
    }
}


/**
 * Log info with additional metadata
 */
public fun logInfo(message: String, builderAction: MutableList<Pair<String, String>>.() -> Unit) {
    val metadata: List<Pair<String, String>> = buildList(builderAction)
    mdc(*metadata.toTypedArray()) {
        defaultLogger.info(message)
    }
}