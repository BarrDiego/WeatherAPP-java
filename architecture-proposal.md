# WeatherApp — resumen de arquitectura y propuesta

**Estado:** propuesta consolidada para la etapa 1  
**Alcance:** modernización del backend; el frontend se abordará en la etapa 2 y los mapas meteorológicos en la etapa 3.

## 1. Objetivo

Modernizar el backend de WeatherApp para Java 25 y Spring Boot 3 y ofrecer una API REST que consulte información meteorológica para una ubicación indicada por la persona usuaria. La aplicación debe mantener un contrato propio, aislar las integraciones externas y degradarse de forma controlada cuando los proveedores gratuitos fallen o alcancen sus límites.

Esta propuesta toma como decisiones aprobadas el uso de **Adapter**, **Strategy**, **Circuit Breaker**, OpenWeather como proveedor principal, Open-Meteo como proveedor secundario y reintentos limitados. Las API/productos concretos y algunos detalles funcionales siguen pendientes de verificación y selección.

## 2. Arquitectura actual resumida

El proyecto es un monolito pequeño basado en Spring MVC, JSP y Spring Boot. La separación por capas es incompleta: `ApiController` maneja la entrada web y crea directamente `ApiClient`; este último mezcla la construcción de solicitudes, el acceso HTTP, la autenticación y el parseo del JSON externo. No existe una capa de aplicación ni una interfaz que desacople el caso de uso del proveedor. `WeatherData` no participa en el flujo actual y refleja estructuras externas con tipos genéricos.

La configuración combina autoconfiguración de Spring Boot con configuración MVC/XML y servlet heredados. La migración a Spring Boot 3 requiere revisar dependencias y sustituir las APIs `javax` pertinentes por `jakarta`. También se debe comprobar la compatibilidad concreta entre Java 25, la versión seleccionada de Spring Boot 3 y sus dependencias.

El flujo actual no transmite de forma fiable la ubicación recibida: el controlador calcula una URL pero no utiliza el resultado, y el cliente consulta a través de un `CityData` estático distinto del formulario. Además, el cliente devuelve JSON crudo en lugar de una respuesta propia de WeatherApp. Existe una credencial literal en el código heredado; no debe reutilizarse ni publicarse. Si aún fuera válida, corresponde revocarla y rotarla.

## 3. Decisiones de arquitectura

### 3.1 Monolito modular con capas simples

Se conserva un solo despliegue y se organizan responsabilidades en módulos/capas. No se propone microservicios, base de datos ni una capa de dominio compleja: no hay requisitos actuales que justifiquen esa carga.

### 3.2 API REST propia

La etapa 1 expone una API REST; las vistas JSP dejan de ser una interfaz soportada. La etapa 2 usará React en `WCSFront`, con un mapa interactivo para seleccionar ubicaciones, buscar lugares y, opcionalmente, usar la ubicación del dispositivo. El frontend consumirá la API REST existente; la selección de coordenadas y la geocodificación permanecen del lado del frontend.

### 3.3 Adapter para proveedores externos

Cada integración implementa un adaptador que traduce entre el contrato interno de WeatherApp y la API específica del proveedor. URL, autenticación, protocolo, DTO externo y errores específicos quedan encapsulados en el adaptador. Así, el resto de la aplicación no depende del JSON ni de los detalles de OpenWeather u otro proveedor.

### 3.4 Strategy para seleccionar proveedor y fallback

Cada proveedor se representa como una estrategia compatible con una interfaz común. Un coordinador de consulta invoca primero OpenWeather y, ante los fallos que la política considere recuperables, prueba Open-Meteo como estrategia secundaria. El orden y los errores que activan el fallback deben configurarse y documentarse; no deben quedar como condicionales dispersos por el controlador o el dominio.

Strategy tiene un propósito concreto porque el requisito incluye dos proveedores intercambiables. La política de selección se mantiene pequeña: proveedor principal y secundario configurados, sin selector dinámico de usuario ni catálogo extensible hasta que exista tal necesidad.

### 3.5 Resiliencia por proveedor

Cada integración tendrá timeout y Circuit Breaker independiente. Se permiten reintentos acotados solo para fallos transitorios, respetando cuotas y condiciones de cada proveedor. No se reintentan errores de entrada ni errores de configuración indefinidamente. Si no se obtiene una respuesta válida de ninguno de los dos proveedores, la API devuelve un error controlado, sin fabricar datos ni filtrar detalles sensibles.

Una dependencia como Resilience4j puede evaluarse para implementar estos mecanismos; la elección queda sujeta a verificar compatibilidad con la versión de Spring Boot seleccionada y a justificar que no resulte innecesaria.

## 4. Componentes propuestos

| Componente | Responsabilidad |
|---|---|
| API REST | Recibir parámetros de ubicación, validar la solicitud y devolver el contrato público de WeatherApp. |
| Manejo de errores API | Convertir errores de validación, ubicación y proveedores a respuestas consistentes y comprensibles. |
| Caso de uso de consulta | Coordinar la consulta y devolver el modelo meteorológico interno. |
| Coordinador de proveedores | Aplicar la selección principal/secundario, los criterios de fallback y la política de resiliencia. |
| Interfaz de proveedor | Definir la capacidad de consulta que requiere WeatherApp sin exponer detalles del protocolo. |
| Adaptador de cada proveedor | Ejecutar solicitudes externas y mapear respuestas y fallos a tipos internos. |
| Modelos internos/API | Representar ubicación y respuesta de WeatherApp, separados de los DTO crudos externos. |
| Configuración | Suministrar endpoints, credenciales, timeouts y políticas mediante configuración externa al código. |

Organización lógica orientativa:

```text
api/
application/
domain/
infrastructure/weather/<proveedor>/
configuration/
```

Los nombres finales pueden ajustarse a las convenciones que se adopten durante la refactorización.

### 4.1 Modelo inicial de condiciones actuales

El contrato público de WeatherApp será independiente del JSON de los proveedores. La respuesta inicial representará una ubicación y una observación actual con:

| Campo | Tipo conceptual | Unidad/formato |
|---|---|---|
| `location.latitude` | decimal | grados |
| `location.longitude` | decimal | grados |
| `location.timezone` | texto | identificador de zona horaria |
| `observedAt` | fecha-hora | instante ISO-8601 |
| `temperatureC` | decimal | °C |
| `feelsLikeC` | decimal | °C |
| `conditionDescription` | texto | español por defecto; idioma opcional |
| `pressureHpa` | decimal | hPa |
| `humidityPercent` | entero/decimal | % |
| `windSpeedMps` | decimal | m/s |
| `windDirectionDegrees` | entero/decimal | grados |
| `cloudCoverPercent` | entero/decimal | % |
| `visibilityMeters` | entero | m |

Un proveedor puede no ofrecer todos los campos; los no disponibles serán opcionales, no se inventarán ni se sustituirán por valores ficticios. Las conversiones y el mapeo se realizan en cada adapter. El contrato usa °C, hPa y m/s independientemente de las unidades nativas del proveedor.

WeatherApp usará español como idioma predeterminado y aceptará un parámetro opcional `lang`. Los adaptadores solicitarán o producirán la descripción en el idioma elegido, sujeto a las capacidades de cada proveedor.

El adapter de OpenWeather mapeará la observación actual de `data` y tomará su descripción de la condición meteorológica principal. Convertirá `dt` a ISO-8601 y, si están presentes, conservará los IDs de `alerts` como referencias internas sin consultar el endpoint de detalle ni incluirlas en el contrato funcional. Si no hay observación actual válida, se tratará como respuesta externa no válida.

El ejemplo de respuesta compartido requiere validación contra una respuesta real controlada: contiene `temp: 286.42` aunque la solicitud indica `units=metric`, y le falta una coma antes de `alerts`. La implementación debe confirmar la unidad efectiva del endpoint antes de convertir temperaturas, no inferirla de ese ejemplo.

La probabilidad de precipitación no pertenece a este modelo de condiciones actuales; se evaluará junto con un modelo de pronóstico en una etapa futura.

### 4.2 Integración Open-Meteo y SDK Java

Open-Meteo será la estrategia secundaria para condiciones actuales. Su adapter consultará el endpoint de Weather Forecast API con coordenadas, zona horaria/variables acordadas y `format=flatbuffers`, y usará el SDK Java oficial `com.open-meteo:sdk` para decodificar la respuesta. La documentación del SDK consultada muestra `1.4.0`; al implementar se debe confirmar la versión vigente en Maven Central y fijarla explícitamente en Maven.

Dependencia Maven (confirmar la versión publicada al comenzar la implementación):

```xml
<dependency>
    <groupId>com.open-meteo</groupId>
    <artifactId>sdk</artifactId>
    <version>1.4.0</version>
</dependency>
```

El SDK **no realiza llamadas HTTP**. El adapter debe usar un cliente HTTP de la aplicación para efectuar la solicitud, aplicar timeout y comprobar el estado HTTP antes de decodificar. El SDK aporta los esquemas y clases FlatBuffers compiladas bajo `com.openmeteo.sdk`, entre ellas `WeatherApiResponse`, `Variable`, `VariableWithValues`, `VariablesWithTime` y `VariablesSearch`.

El flujo de decodificación descrito por el README es:

1. Solicitar `format=flatbuffers`; la respuesta predeterminada de la API es JSON y no debe enviarse al decodificador FlatBuffers.
2. Leer el cuerpo binario completo y crear un `ByteBuffer` en orden `LITTLE_ENDIAN`.
3. La respuesta meteorológica es size-prefixed: el ejemplo Java posiciona el buffer después de los primeros 4 bytes antes de llamar a `WeatherApiResponse.getRootAsWeatherApiResponse(...)`.
4. Leer `current()` y buscar cada variable solicitada mediante `VariablesSearch`/`Variable`; filtrar también por `altitude` cuando aplique, como para temperatura a 2 m o viento a una altura específica. No depender del índice de la variable para el mapeo interno.
5. Leer unidades y metadatos del SDK y mapear los valores solicitados al modelo WeatherApp; convertir o rechazar unidades inesperadas explícitamente.

La respuesta incluye coordenadas del punto de grilla, elevación, zona horaria y offset, además de bloques `current`, `hourly`, `daily` y otros según las variables solicitadas. Los timestamps del SDK son Unix en segundos y están en GMT; para el modelo de WeatherApp deben representarse como instantes ISO-8601. El endpoint de Geocoding no tiene esquema disponible en este SDK, por lo que su elección/implementación se mantiene separada de este adapter meteorológico.

La petición `current=` debe enumerar explícitamente las variables que requiere el mapeo, después de verificar cuáles acepta el endpoint actual de Open-Meteo. La disponibilidad puede diferir de OpenWeather; no se deben asumir índices ni paridad de campos. El código meteorológico WMO de Open-Meteo es numérico y no una descripción localizada, por lo que el adapter debe traducirlo a una descripción comprensible según `lang`, con comportamiento definido para códigos desconocidos.

Las pruebas del adapter deben usar payloads FlatBuffers de muestra/controlados y cubrir cuerpo vacío o malformado, respuesta HTTP no exitosa y variable/unidad ausente o inesperada. No se debe interpretar una respuesta JSON de error como si fuera un bloque FlatBuffers.

Referencias consultadas: [README general del SDK](https://github.com/open-meteo/sdk/blob/main/README.md), [README del SDK Java](https://github.com/open-meteo/sdk/blob/main/java/README.md) y [Weather Forecast API](https://open-meteo.com/en/docs).

## 5. Flujo de consulta

1. El frontend envía latitud y longitud; la obtención/selección de esas coordenadas queda a cargo del frontend. La solicitud puede incluir un idioma opcional; el idioma predeterminado es español.
2. La API valida que latitud y longitud estén presentes, sean numéricas y estén dentro de sus rangos válidos. Una entrada inválida se rechaza antes de consumir cuota del proveedor.
3. El caso de uso solicita información al coordinador.
4. El coordinador invoca OpenWeather a través de su estrategia. El adaptador aplica timeout y Circuit Breaker, efectúa la llamada y transforma la respuesta externa.
5. Si ocurre un fallo clasificado como recuperable por la política acordada, el coordinador intenta Open-Meteo bajo sus propios límites de resiliencia.
6. El caso de uso devuelve el modelo interno y la API lo convierte al contrato público de WeatherApp.
7. Si la consulta no puede completarse, el manejo de errores devuelve un error estable y comprensible, sin credenciales, JSON externo ni detalles internos sensibles.

## 6. Errores y política de fallback

La aplicación debe distinguir al menos entre coordenadas inválidas, proveedor sin datos para las coordenadas, fallo transitorio del proveedor, límite de uso, timeout, respuesta externa no válida y configuración ausente o incorrecta.

Se acuerda usar Open-Meteo como proveedor secundario de OpenWeather y reintentos limitados, pero falta definir exactamente qué fallos habilitan el cambio de proveedor. Como criterio inicial, conviene restringirlo a fallos transitorios (por ejemplo, timeout o indisponibilidad) y revisar por separado límites de uso, errores de autenticación, respuesta inválida y ubicación no encontrada. Un fallo de configuración no debería quedar oculto por un fallback silencioso. Si ambos proveedores fallan, se responde con un error controlado.

El número de reintentos, sus intervalos y los umbrales de Circuit Breaker se definirán después de elegir los proveedores y revisar sus cuotas. Se evitarán reintentos agresivos que multipliquen solicitudes en planes gratuitos.

## 7. Pruebas y validación

- **Unitarias:** JUnit y Mockito para la lógica de aplicación, validación, selección/fallback y mapeos relevantes. Se usan dobles de prueba; no se llama a servicios reales.
- **Integración de adaptadores:** respuestas HTTP controladas para comprobar solicitudes, mapeo, errores y timeout sin credenciales reales.
- **Resiliencia:** pruebas de apertura/recuperación de cada Circuit Breaker, reintentos acotados, fallback al secundario y error final si ambos proveedores fallan.
- **Plataforma:** compilar y ejecutar con Java 25 y la versión de Spring Boot 3 elegida; validar la configuración limpia de MVC y el stack Jakarta.
- **Code review:** revisar los cambios con `/r`, resolver los hallazgos acordados y registrar decisiones relevantes.

Las pruebas necesarias se escribirán durante el desarrollo; la fase final de pruebas unitarias verifica la cobertura acordada. La validación de adaptadores y la revisión de código complementan, pero no reemplazan, las pruebas unitarias.

## 8. Fases de trabajo

1. **Etapa 1 — backend:** migrar a Java 25 y Spring Boot 3; retirar o adaptar JSP y configuración heredada que no corresponda a REST; implementar la API y el flujo de consulta con OpenWeather principal y Open-Meteo secundario, usando Adapter, Strategy, Circuit Breaker y reintentos limitados. Documentar decisiones y validar con pruebas.
2. **Etapa 2 — frontend:** implementar la interfaz React definida en `frontend-spec.md`. React-Leaflet y OpenStreetMap (OSM) son candidatos para el mapa base, sujetos a verificar sus requisitos de uso y atribución. El mapa permite seleccionar ubicaciones y no incorpora capas meteorológicas en esta etapa.
3. **Etapa 3 — mapas meteorológicos:** agregar mapas de radar de precipitaciones u otras capas meteorológicas que estén disponibles en los planes gratuitos de las API seleccionadas, después de comprobar cobertura, actualización y condiciones de uso.
4. **Despliegue:** desplegar los componentes en un servicio online gratuito por seleccionar, sujeto a validar límites, compatibilidad y condiciones de operación.
5. **Code review y pruebas:** revisar los cambios con `/r`; completar JUnit/Mockito para comportamiento principal, validación, errores y resiliencia, además de pruebas de integración HTTP controlada de los adaptadores.

El orden define las entregas de trabajo, pero no obliga a posponer pruebas hasta la cuarta fase.

## 9. Decisiones pendientes

1. Verificar las API/productos concretos de OpenWeather y Open-Meteo, su compatibilidad con los campos normalizados, cobertura argentina, cuotas, coste y condiciones de uso; confirmar la versión publicada del SDK Java Open-Meteo.
2. Definir los fallos que habilitan el fallback, los reintentos máximos, sus intervalos y umbrales del Circuit Breaker, a partir de las políticas reales de ambos proveedores.
3. Elegir versiones concretas de Java/Spring Boot 3 y dependencias tras verificar compatibilidad con Java 25.
4. En una etapa futura, definir el modelo de pronóstico y el horizonte de la probabilidad de precipitación.
5. En etapa 2, cerrar el diseño visual, seleccionar biblioteca/proveedor de mapa y geocodificador, definir el centro inicial y la interacción de consulta; verificar cobertura, cuotas, atribución y condiciones de uso.
6. En etapa 3, seleccionar las capas meteorológicas de radar de precipitaciones u otras que permitan los planes gratuitos de las API elegidas.
7. Seleccionar el servicio de hosting gratuito y verificar sus límites y requisitos de despliegue.

Estas decisiones no cambian la arquitectura general propuesta, pero sí condicionan el contrato final, la configuración y los parámetros operativos.

## 10. Fuera de alcance de la etapa 1

- Frontend nuevo o mantenimiento de JSP como interfaz soportada.
- Persistencia, usuarios o almacenamiento de búsquedas.
- Mapas meteorológicos y nuevas funcionalidades todavía no especificadas.
- Selección interactiva de proveedor por la persona usuaria.
- Reintentos ilimitados, caché de datos meteorológicos o generación de respuestas cuando ambos proveedores fallan.
