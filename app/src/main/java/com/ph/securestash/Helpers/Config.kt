package com.ph.securestash.Helpers

import org.json.JSONObject
import java.io.File

object Config {
    fun load(cacheDir: File): JSONObject {
        val configDir = File(cacheDir, "config.json")
        return if (configDir.exists()) {
            val content = configDir.readText()
            if (content.isNotBlank()) JSONObject(content) else JSONObject()
        } else {
            JSONObject()
        }
    }

    fun update(cacheDir: File, key: String, value: String) {
        val configDir = File(cacheDir, "config.json")
        val config = if (configDir.exists()) {
            val content = configDir.readText()
            if (content.isNotBlank()) JSONObject(content) else JSONObject()
        } else {
            JSONObject()
        }

        config.put(key, value)
        configDir.writeText(config.toString())
    }

    fun remove(cacheDir: File, key: String) {
        val configDir = File(cacheDir, "config.json")
        val config = if (configDir.exists()) {
            val content = configDir.readText()
            if (content.isNotBlank()) JSONObject(content) else JSONObject()
        } else {
            JSONObject()
        }

        config.remove(key)
    }
}