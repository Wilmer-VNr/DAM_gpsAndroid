package com.jzaldumbide.test.gpsandroid

import android.content.Context
import android.util.Log
import android.widget.Toast
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

object SupabaseHelper {
    private val supabaseUrl = "https://kldhwzmvasgzlbbaxicl.supabase.co/rest/v1/locations"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtsZGh3em12YXNnemxiYmF4aWNsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTI2MDY2NTYsImV4cCI6MjA2ODE4MjY1Nn0.RGUgnqrltINpoEKr6EfZ8Ry7DRwufZanjpPxyGc5hKo"
    
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        engine {
            requestTimeout = 60000 // 60 segundos
        }
    }
    
    fun enviarUbicacion(context: Context, latitude: Double, longitude: Double) {
        val json = """
            {
                "id": "${java.util.UUID.randomUUID()}",
                "timestamp": "${java.time.Instant.now()}",
                "latitude": $latitude,
                "longitude": $longitude
            }
        """.trimIndent()
        
        Log.d("SupabaseHelper", "Enviando: $json")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = httpClient.post(supabaseUrl) {
                    contentType(ContentType.Application.Json)
                    header("apikey", supabaseKey)
                    header("Authorization", "Bearer $supabaseKey")
                    header("Prefer", "return=minimal")
                    setBody(json)
                }
                
                withContext(Dispatchers.Main) {
                    if (response.status.value in 200..299) {
                        Toast.makeText(
                            context,
                            "Ubicación enviada: $latitude, $longitude",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Error: ${response.status.value}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("SupabaseHelper", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error al enviar datos: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    fun cerrar() {
        httpClient.close()
    }
}