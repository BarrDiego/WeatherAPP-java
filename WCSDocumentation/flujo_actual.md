Flujo actual: consulta de condiciones meteorológicas

[Frontend: mapa, búsqueda o geolocalización]
       │ Selecciona ubicación y confirma
       │ (búsqueda de ciudad se resuelve a coordenadas en el frontend)
       ▼
[GET /api/weather?latitude=...&longitude=...&lang=es]
       │
       ▼
[WeatherController: valida solicitud]
       │
       ▼
[WeatherQueryService]
       │
       ▼
[Circuit Breaker OpenWeather]
       ├── CLOSED / HALF_OPEN ──► [OpenWeatherProvider] ──► [OpenWeather API]
       │                               │ éxito
       │                               └────────────────────────► respuesta normalizada
       │
       └── OPEN o fallo recuperable
                       │
                       ▼
             [Circuit Breaker Open-Meteo]
                ├── CLOSED / HALF_OPEN ──► [OpenMeteoProvider]
                │                              │ HTTP FlatBuffers
                │                              ▼
                │                        [Open-Meteo API]
                │                              │
                │                              ▼
                │                       [SDK decodifica]
                │                              │
                │                              └──────────────► respuesta normalizada
                │
                └── OPEN o fallo ──► [WeatherProviderException]
                                             │
                                             ▼
                                  [ApiExceptionHandler: 502]
                                             │
                                             ▼
                                  [Frontend: muestra la card
                                   o un error recuperable]

Política Circuit Breaker (por proveedor):
- Ventana deslizante: 10 llamadas; evaluación desde 5 llamadas.
- Umbral: 50 % de fallos recuperables.
- OPEN: espera 30 s; luego HALF_OPEN permite 2 llamadas de prueba.
- Los errores no recuperables (por ejemplo, credenciales rechazadas) no se registran
  como fallo del circuito ni activan fallback.
- La solicitud HTTP también tiene timeout de conexión (2 s) y lectura (5 s).
- No hay política de reintentos implementada actualmente.