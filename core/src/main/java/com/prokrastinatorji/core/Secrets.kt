package com.prokrastinatorji.core

import java.io.InputStream
import java.util.Properties

object Secrets {
    private val properties = Properties()

    init {
        try {
            val inputStream: InputStream? = javaClass.classLoader?.getResourceAsStream("secrets.properties")
            if (inputStream != null) {
                properties.load(inputStream)
            } else {
                println("WARNING: secrets.properties not found. API calls requiring keys will fail.")
            }
        } catch (e: Exception) {
            println("ERROR: Could not load secrets.properties. ${e.message}")
        }
    }

    fun getGoogleApiKey(): String? {
        return properties.getProperty("GOOGLE_API_KEY")
    }
}
