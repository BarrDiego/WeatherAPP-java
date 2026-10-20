# WeatherApp — Resumen de implementación

## Estado de la etapa 1

La primera etapa del backend quedó modernizada y validada. La aplicación pasó de un proyecto legado basado en Spring Boot 2 / Java 8 con MVC y JSP a una API REST moderna con Java 25 y Spring Boot 3.5.

La API expone un endpoint principal:

- GET /api/weather?latitude={valor}&longitude={valor}

La respuesta devuelve un modelo propio de WeatherApp, separado del JSON externo de los proveedores meteorológicos.

## Objetivo logrado

Se construyó un backend funcional para consultar condiciones meteorológicas actuales usando:

- OpenWeather como proveedor principal
- Open-Meteo como proveedor secundario
- validación de coordenadas y errores consistentes
- modelo interno independiente del proveedor externo
- arquitectura preparada para crecer sin acoplar el negocio a un cliente concreto

## Decisiones técnicas tomadas

### 1. Modernización de la base tecnológica

Se actualizó la app a Java 25 y Spring Boot 3.5, con una configuración más limpia y compatible con el ecosistema moderno.

Esto fue necesario porque la versión original mezclaba:

- Spring MVC clásico
- vistas JSP
- configuración heredada
- llamadas HTTP directas a la API externa
- DTOs acoplados al JSON del proveedor

Eso hacía muy difícil mantener, probar y extender la lógica.

## 2. API REST propia y modelo interno

En lugar de devolver el JSON crudo del proveedor, se definió un modelo interno de WeatherApp con un contrato estable para la aplicación.

Esto permite que el backend sea independiente de la estructura exacta de OpenWeather o Open-Meteo. Si un proveedor cambia y su esquema cambia, la capa externa se adapta allí, no en toda la lógica del negocio.

El modelo interno incluye:

- ubicación
- zona horaria
- fecha de observación
- temperatura y sensación térmica
- descripción del clima
- presión
- humedad
- viento
- nubosidad
- visibilidad

## 3. Adaptadores por proveedor

Se introdujo el patrón Adapter para encapsular cada integración externa.

### ¿Por qué?

Cada proveedor tiene:

- endpoint distinto
- formato de respuesta distinto
- unidades distintas
- nombres distintos de propiedades
- errores y casos edge diferentes

El adapter hace la traducción desde la respuesta externa hasta el modelo de WeatherApp.

### Beneficio

La aplicación no necesita saber detalles de implementación de OpenWeather ni Open-Meteo. Solo conoce el contrato interno del dominio.

## 4. Strategy para cambiar de proveedor

Se definió una interfaz común `WeatherProvider` y dos implementaciones:

- `OpenWeatherProvider`
- `OpenMeteoProvider`

Esto permite aplicar el patrón Strategy para elegir el proveedor según la política de negocio.

### ¿Por qué Strategy?

Porque el sistema tiene dos alternativas válidas para cumplir la misma operación: “obtener clima actual”.

La lógica de selección queda desacoplada de la lógica de negocio y cuando se agrega un nuevo proveedor solo se agrega una nueva implementación, sin reescribir la lógica del servicio principal.

### Vale la pena

Sí, porque hay una diferencia real entre “hacer una búsqueda con un proveedor concreto” y “consultar clima usando quien esté disponible según la política definida”. Eso es un caso de negocio real, no un patrón agregado artificialmente.

## 5. Fallback y resiliencia

La capa de servicio ejecuta la consulta del proveedor principal. Si se detecta un fallo recuperable, intenta con el proveedor secundario.

### ¿Por qué fallback?

Las APIs externas no son 100% fiables:

- timeouts
- errores temporales
- caídas de servicio
- límites de uso
- respuesta incompleta

Si el backend depende de una sola fuente, se vuelve frágil.

### Resultado

La aplicación puede degradar de forma controlada en vez de romper la experiencia completa cuando una fuente falla.

## 6. Validación de entrada y manejo de errores

Se agregaron validaciones para latitud y longitud:

- latitud entre -90 y 90
- longitud entre -180 y 180
- parámetros requeridos

Esto evita que la aplicación consuma cuota de proveedor con datos inválidos.

También existe un manejador global de errores (`ApiExceptionHandler`) para devolver respuestas estables y comprensibles.

## 7. Principios SOLID aplicados

### SRP — Single Responsibility Principle
Cada clase tiene una función clara:

- `WeatherController`: expone la API REST
- `WeatherQueryService`: coordina la lógica
- `OpenWeatherProvider`: consulta OpenWeather
- `OpenMeteoProvider`: consulta Open-Meteo
- `ApiExceptionHandler`: formatea errores

Esto reduce el acoplamiento y facilita cambios puntuales.

### OCP — Open/Closed Principle
La lógica del caso de uso no necesita modificarse para agregar un nuevo proveedor. Basta con implementar la misma interfaz.

### LSP — Liskov Substitution Principle
Cualquier proveedor que implemente `WeatherProvider` debe poder sustituir al otro sin romper el comportamiento esperado del sistema.

### ISP — Interface Segregation Principle
La interfaz es pequeña y específica: “dame clima actual”. No se agregaron métodos extra para cada caso de uso inexistente.

### DIP — Dependency Inversion Principle
La capa de servicio depende de una abstracción (`WeatherProvider`) y no de concretos.

Eso permite:

- testing más simple
- mocks y stubs fáciles
- cambios de implementación sin romper la lógica del servicio

## 8. Qué aprendimos de la arquitectura

La clave de la calidad no está en “usar muchos patrones”, sino en usar los adecuados cuando hay un problema real.

En este caso, los patrones ayudaron porque había dos necesidades concretas:

1. dos proveedores externos con contratos distintos,
2. una lógica de negocio que no debía depender del proveedor elegido.

Eso es diseño de software con propósito.

## 9. Qué queda para la siguiente etapa

La etapa 1 quedó bien fundamentada para avanzar.

Las siguientes decisiones naturales son:

- definir pruebas más exhaustivas por proveedor,
- agregar política más explícita de timeout/reintentos/circuit breaker,
- validar el fallback con escenarios reales controlados,
- cerrar el diseño y seleccionar los servicios de mapa y geocodificación del frontend React de la etapa 2 (`frontend-spec.md`),
- preparar la etapa 3 con mapas meteorológicos

## 10. Conclusión

La implementación de esta etapa sirvió como ejemplo concreto de diseño orientado a dominio, separación de responsabilidades y adaptación a proveedores externos.

La principal idea fue simple y muy útil para aprender:

- no mezclar negocio, HTTP y proveedor externo,
- no acoplar la aplicación a un formato ajeno,
- separar la capa de entrada, la lógica de dominio y la infraestructura externa.

Eso es la esencia de una API mantenible y extensible.
