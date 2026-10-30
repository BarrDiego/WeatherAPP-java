[ Agentic Router / LLM ]
                                 (Evalúa la intención)
                                           │
                                  ¿Qué necesito traer?
                                           │
                  ┌────────────────────────┴────────────────────────┐
                  ▼                                                 ▼
       [ OpenWeatherAdapter ]                            [ MeteoRedAdapter ]
(Datos generales, temp, humedad)                  (Probabilidad de lluvia refinada, radar)
                  │                                                 │
                  └────────────────────────┬────────────────────────┘
                                           │
                                           ▼
                                 [ Agente Sintetizador ]
                     (Combina los JSONs y arma la respuesta final)