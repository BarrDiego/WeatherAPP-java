Actúa como un Java Tech Lead y Arquitecto de Software Senior especializado en Arquitectura Hexagonal y GenAI.

Necesito diseñar e implementar la estructura del backend para la aplicación WCSApp en Java (Spring Boot) utilizando el framework LangChain4j (o la abstracción de Tool Calling correspondiente).

---

### CONTEXTO Y OBJETIVO
Estamos diseñando una capa agéntica que interpreta la intención del usuario y coordina capacidades meteorológicas existentes. OpenWeather es el proveedor principal y Open-Meteo el fallback. El backend conserva la responsabilidad de ejecutar la política de selección y resiliencia; el agente no debe eludirla ni duplicar el fallback.

La arquitectura debe basarse en Arquitectura Hexagonal y los patrones Strategy y Adapter.

---

### TAREAS A EJECUTAR (PLAN DE TRABAJO)

Por favor, genera el código limpio, bien documentado y estructurado para los siguientes pasos:

1. PUERTOS Y DOMINIO (Domain / Ports):
   - Crear una interfaz de puerto de salida `WeatherProviderPort` con métodos estándar como `getWeatherSummary(double lat, double lon)` y `getDetailedForecast(double lat, double lon)`.
   - Definir los DTOs de dominio inmutables (`WeatherData`, `ForecastData`).

2. ADAPTADORES SECUNDARIOS (Adapters / Infrastructure):
   - Crear `OpenWeatherAdapter` implementando `WeatherProviderPort` (simular o estructurar la llamada REST HTTP).
   - Crear `OpenMeteoAdapter` implementando `WeatherProviderPort` (simular o estructurar la llamada REST HTTP), alineado con el contrato actual de condiciones meteorológicas.

3. HERRAMIENTAS PARA EL AGENTE (Tool Calling / LangChain4j Annotations):
   - Anotar o envolver los métodos de los adaptadores para que puedan ser expuestos como `@Tool` reconocibles por el agente de LLM.
   - Incluir descripciones claras en las `@Tool` para que el LLM entienda las capacidades disponibles. No atribuir a Open-Meteo pronóstico detallado, alertas o radar si no están implementados en el backend.

4. SERVICIO Y ORQUESTACIÓN (Strategy Context + Fallback):
   - Crear el servicio de aplicación `WeatherAgentService`.
   - Configurar la interfaz del agente de LangChain4j (`@AiService`) que conecta las `Tools` con el modelo (LLM).
   - Mantener el fallback ante Rate Limit, timeout u otros errores recuperables dentro de la política de proveedores del backend. El agente debe presentar errores recuperables sin filtrar detalles internos y sin implementar una segunda política de fallback.

---

Por favor, comienza proponiendo la estructura de paquetes recomendada y luego genera el código paso a paso comenzando por los Puertos y DTOs de Dominio.