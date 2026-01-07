package com.prokrastinatorji.core

import java.io.InputStream
import java.util.Properties

object Secrets {
    private val properties = Properties()
    private var isLoaded = false

    fun loadFromInputStream(inputStream: InputStream?) {
        if (isLoaded) return
        try {
            if (inputStream != null) {
                properties.load(inputStream)
                isLoaded = true
            } else {
                println("WARNING: InputStream for secrets is null. API calls requiring keys will fail.")
            }
        } catch (e: Exception) {
            println("ERROR: Could not load secrets from InputStream. ${e.message}")
        }
    }

    fun getGoogleApiKey(): String? {
        return properties.getProperty("GOOGLE_API_KEY")
    }
}
