package com.jzaldumbide.test.gpsandroid

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import java.util.*
import java.time.Instant

@Serializable
data class LocationData(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String = java.time.Instant.now().toString(),
    val latitude: Double,
    val longitude: Double
)

class MainActivity : AppCompatActivity() {

    private lateinit var tvMensaje: TextView
    private lateinit var btnIniciar: Button
    private val handler = Handler(Looper.getMainLooper())
    private var contador = 1
    private var contadorActivo = false
    
    // Este cliente HTTP ya está definido más abajo
    
    // URL de Supabase y clave API
    private val supabaseUrl = "https://kldhwzmvasgzlbbaxicl.supabase.co/rest/v1/locations"
    private val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtsZGh3em12YXNnemxiYmF4aWNsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTI2MDY2NTYsImV4cCI6MjA2ODE4MjY1Nn0.RGUgnqrltINpoEKr6EfZ8Ry7DRwufZanjpPxyGc5hKo"
    
    // Configuración del cliente HTTP
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        engine {
            requestTimeout = 60000 // 60 segundos
        }
    }

    private val mensajeRunnable = object : Runnable {
        override fun run() {
            tvMensaje.text = "Mensaje número $contador"
            
            // Opción 1: Usar ubicación real
            enviarUbicacion()
            
            // Opción 2: Usar ubicación fija para pruebas
            // Descomentar esta línea y comentar enviarUbicacion() para pruebas
            // SupabaseHelper.enviarUbicacion(this@MainActivity, -0.1801, -78.4678)
            
            contador++
            handler.postDelayed(this, 1000) // 1 segundo
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Permisos concedidos, ahora el usuario puede iniciar el contador con el botón
                Toast.makeText(this, "Permisos concedidos. Presiona Iniciar para comenzar", Toast.LENGTH_LONG).show()
                btnIniciar.isEnabled = true
            }
            else -> {
                Toast.makeText(this, "Se requieren permisos de ubicación", Toast.LENGTH_LONG).show()
                btnIniciar.isEnabled = false
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvMensaje = findViewById(R.id.tvMensaje)
        btnIniciar = findViewById(R.id.btnIniciar)
        
        btnIniciar.setOnClickListener {
            if (!contadorActivo) {
                // Iniciar el contador
                contadorActivo = true
                contador = 1
                handler.post(mensajeRunnable)
                btnIniciar.text = "Detener"
            } else {
                // Detener el contador
                contadorActivo = false
                handler.removeCallbacks(mensajeRunnable)
                btnIniciar.text = "Iniciar"
                tvMensaje.text = "Contador detenido"
            }
        }
        
        // Solicitar permisos de ubicación
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    private fun enviarUbicacion() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            val cancellationToken = CancellationTokenSource()
            
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            ).addOnSuccessListener { location: Location? ->
                location?.let {
                    val latitude = it.latitude
                    val longitude = it.longitude
                    
                    // Enviar datos a Supabase usando el helper
                    SupabaseHelper.enviarUbicacion(this, latitude, longitude)
                    
                    // O usando el método original
                    // enviarDatosASupabase(latitude, longitude, contador)
                }
            }.addOnFailureListener { e ->
                Toast.makeText(
                    this@MainActivity,
                    "Error al obtener ubicación: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun enviarDatosASupabase(latitude: Double, longitude: Double, count: Int) {
        val locationData = LocationData(
            latitude = latitude,
            longitude = longitude
        )
        
        Log.d("Supabase", "Enviando ubicación: $latitude, $longitude")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("Supabase", "Iniciando petición HTTP a $supabaseUrl")
                val response = httpClient.post(supabaseUrl) {
                    contentType(ContentType.Application.Json)
                    header("apikey", supabaseKey)
                    header("Authorization", "Bearer $supabaseKey")
                    header("Prefer", "return=minimal")
                    setBody(locationData)
                }
                
                Log.d("Supabase", "Respuesta: ${response.status.value}")
                
                withContext(Dispatchers.Main) {
                    if (response.status.value in 200..299) {
                        Log.d("Supabase", "Ubicación enviada correctamente")
                        Toast.makeText(
                            this@MainActivity,
                            "Ubicación enviada: $latitude, $longitude",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Log.e("Supabase", "Error HTTP: ${response.status.value}")
                        Toast.makeText(
                            this@MainActivity,
                            "Error: ${response.status.value}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("Supabase", "Excepción: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error al enviar datos: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(mensajeRunnable) // Evita fugas de memoria
        httpClient.close()
        SupabaseHelper.cerrar()
    }
    
    override fun onPause() {
        super.onPause()
        // Detener el contador si la app pasa a segundo plano
        if (contadorActivo) {
            contadorActivo = false
            handler.removeCallbacks(mensajeRunnable)
            btnIniciar.text = "Iniciar"
        }
    }
}