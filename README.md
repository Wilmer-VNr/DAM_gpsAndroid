# Aplicación GPS con Supabase

Esta aplicación Android muestra un contador que se incrementa cada segundo y envía la ubicación del dispositivo a una base de datos Supabase cada vez que el contador aumenta.

## Características

- Muestra un contador que se incrementa cada segundo
- Obtiene la ubicación GPS del dispositivo cuando el contador aumenta
- Envía los datos de ubicación a Supabase

## Configuración

Para usar esta aplicación, necesitas configurar tu proyecto Supabase:

1. Crea una cuenta en [Supabase](https://supabase.com/)
2. Crea un nuevo proyecto
3. Crea una tabla llamada `locations` con la siguiente estructura:
   - `id` (uuid, primary key)
   - `timestamp` (timestamp with time zone)
   - `latitude` (double precision)
   - `longitude` (double precision)

4. Abre el archivo `MainActivity.kt` y actualiza las siguientes líneas con tus credenciales de Supabase:

```kotlin
private val supabaseUrl = "https://tu-proyecto.supabase.co/rest/v1/locations"
private val supabaseKey = "tu-clave-api-supabase"
```

## Permisos

La aplicación requiere los siguientes permisos:
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `INTERNET`
- `ACCESS_NETWORK_STATE`
