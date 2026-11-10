Etapa 3 propuesta: capa agéntica sobre la API meteorológica existente

[Frontend: pregunta del usuario]
       │
       ▼
[Backend Agent API / WeatherAgentService]
       │
       ▼
[Agente LLM: interpreta intención y decide si consultar clima actual]
       │
       ├── No requiere datos meteorológicos ──► [Respuesta conversacional]
       │
       └── Requiere condiciones actuales
                    │ Tool calling al caso de uso existente
                    ▼
             [WeatherQueryService]
                    │
                    ├──► [OpenWeather + Circuit Breaker]
                    │         └──► ante fallo recuperable o circuito OPEN
                    │
                    └──► [Open-Meteo + Circuit Breaker]
                              └──► fallback de condiciones actuales,
                                   con decodificación FlatBuffers por el SDK
                    │
                    ▼
             [Contrato normalizado de WCSApp]
                    │
                    ▼
             [Agente sintetiza una respuesta en lenguaje natural]
                    │
                    ▼
             [Frontend presenta la respuesta]

Este diagrama describe el objetivo de la etapa 3; la capa agéntica todavía no está
implementada. El agente debe usar la política de proveedores del backend y no invocar
directamente APIs externas ni duplicar el fallback. El contrato actual expone
condiciones meteorológicas presentes: no asumir pronóstico, alertas ni radar.