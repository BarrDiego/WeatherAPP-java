Actúa como un Java Tech Lead y Arquitecto de Software Senior especializado en Arquitectura Hexagonal y GenAI.

Necesito diseñar e implementar la estructura del backend para la aplicación WCSApp en Java (Spring Boot) utilizando el framework LangChain4j (o la abstracción de Tool Calling correspondiente).

---

### CONTEXTO Y OBJETIVO
Estamos integrando un agente de IA que actúa como un "Dynamic Strategy Router" para decidir qué API meteorológica consultar (OpenWeatherMap o MeteoRed) según la intención del usuario y la necesidad de resiliencia.

La arquitectura debe basarse en Arquitectura Hexagonal y los patrones Strategy y Adapter.

---

### TAREAS A EJECUTAR (PLAN DE TRABAJO)

Por favor, genera el código limpio, bien documentado y estructurado para los siguientes pasos:

1. PUERTOS Y DOMINIO (Domain / Ports):
   - Crear una interfaz de puerto de salida `WeatherProviderPort` con métodos estándar como `getWeatherSummary(double lat, double lon)` y `getDetailedForecast(double lat, double lon)`.
   - Definir los DTOs de dominio inmutables (`WeatherData`, `ForecastData`).

2. ADAPTADORES SECUNDARIOS (Adapters / Infrastructure):
   - Crear `OpenWeatherAdapter` implementando `WeatherProviderPort` (simular o estructurar la llamada REST HTTP).
   - Crear `MeteoRedAdapter` implementando `WeatherProviderPort` (simular o estructurar la llamada REST HTTP).

3. HERRAMIENTAS PARA EL AGENTE (Tool Calling / LangChain4j Annotations):
   - Anotar o envolver los métodos de los adaptadores para que puedan ser expuestos como `@Tool` reconocibles por el agente de LLM.
   - Incluir descripciones claras en cada `@Tool` para que el LLM entienda cuándo invocar OpenWeather (clima general/actual) y cuándo MeteoRed (lluvias/alertas/precisión).

4. SERVICIO Y ORQUESTACIÓN (Strategy Context + Fallback):
   - Crear el servicio de aplicación `WeatherAgentService`.
   - Configurar la interfaz del agente de LangChain4j (`@AiService`) que conecta las `Tools` con el modelo (LLM).
   - Implementar un mecanismo de fallback o try-catch en el servicio en caso de que una herramienta arroje una excepción de Rate Limit (429) o Timeout.

---

Por favor, comienza proponiendo la estructura de paquetes recomendada y luego genera el código paso a paso comenzando por los Puertos y DTOs de Dominio.