# WeatherApp — plan de implementación

**Estado:** listo para iniciar cuando se cierren las decisiones pendientes de la etapa 1  
**Alcance de este plan:** backend REST. Frontend y mapas quedan para etapas posteriores.

## Objetivo

Modernizar el backend a Java 25 y Spring Boot 3 para consultar el clima mediante OpenWeather como proveedor principal y Open-Meteo como proveedor secundario. La API expondrá un contrato propio y manejará fallos de forma consistente, con pruebas que no dependan de servicios ni credenciales reales.

## Decisiones acordadas

- OpenWeather será el proveedor principal; Open-Meteo será el secundario para fallback ante fallos recuperables.
- Se aplicarán Adapter para aislar proveedores, Strategy para coordinarlos y resiliencia independiente por proveedor: timeout, Circuit Breaker y reintentos acotados.
- No se incorpora persistencia.
- El despliegue objetivo es un servicio online gratuito, a seleccionar y validar antes del despliegue.
- Etapa 2: usar React en `WCSFront`, con mapa interactivo, búsqueda de lugares y opción de ubicación del dispositivo; la especificación funcional está en `frontend-spec.md`. React-Leaflet y OpenStreetMap siguen como candidatos de mapa, sujetos a validar condiciones y atribución.
- Etapa 3: agregar mapas de radar de precipitaciones u otras capas meteorológicas disponibles bajo los planes gratuitos de las API seleccionadas.

## Modelo acordado y decisiones pendientes

El frontend enviará latitud y longitud; obtener/seleccionar coordenadas es responsabilidad del frontend. El backend valida los valores y consulta los proveedores con esas coordenadas. El modelo inicial de condiciones actuales está definido en `spec.md` y `architecture-proposal.md`: ubicación (latitud, longitud y zona horaria), hora de observación, temperatura y sensación térmica en °C, descripción, presión en hPa, humedad en %, velocidad del viento en m/s, dirección en grados, nubosidad en % y visibilidad en metros. Las descripciones serán en español por defecto, con parámetro opcional `lang`; los campos no disponibles de un proveedor serán opcionales.

La probabilidad de precipitación queda para un futuro modelo de pronóstico. Los IDs de alertas de OpenWeather se mapearán internamente, sin consultar el endpoint de detalle ni usarlos funcionalmente ahora.

Pendientes antes de iniciar implementación:

1. **Fallback y resiliencia:** fallos que habilitan fallback; reintentos e intervalos; timeouts y umbrales de Circuit Breaker, limitados por las cuotas y condiciones vigentes de los proveedores.
2. **Versiones:** seleccionar una versión concreta de Spring Boot 3 y verificar su compatibilidad con Java 25 y las dependencias requeridas.
3. **Acceso a OpenWeather:** verificar que la suscripción/plan habilite One Call 4.0 (`/data/4.0/onecall/current`) y revisar cuotas y condiciones de uso.

No se deben inventar datos meteorológicos ni ocultar fallos de configuración mediante fallback. Si estas decisiones pendientes siguen abiertas al iniciar, el usuario debe aprobar explícitamente los supuestos propuestos por el `dev-agent`.

## Fases de trabajo

### 1. Preparación y baseline

- Confirmar repositorio y rama de trabajo antes de modificar archivos.
- Revisar `spec.md`, `architecture-proposal.md` y el backend en `WCSApp`.
- Inspeccionar el estado inicial con las herramientas Maven disponibles y registrar incompatibilidades relevantes, sin depender de llamadas meteorológicas reales.
- Tratar la credencial de OpenWeather encontrada en el código heredado como expuesta: revocarla/rotarla y no reutilizarla. No copiar secretos a documentación, tests ni historial.

**Salida:** alcance confirmado, decisiones funcionales cerradas y baseline técnico conocido.

### 2. Modernización del backend

- Actualizar Maven, Java y Spring Boot a las versiones aprobadas.
- Retirar o adaptar JSP, servlet/XML y dependencias heredadas que no correspondan a la API REST.
- Establecer configuración externa para endpoints, credenciales y políticas; incluir únicamente placeholders seguros en ejemplos de configuración.
- Preparar una aplicación ejecutable con configuración compatible con el despliegue objetivo, sin añadir persistencia.

**Gate:** compila y arranca con las versiones objetivo; las credenciales no están en el código.

### 3. API y proveedor principal

- Crear endpoint REST que reciba latitud y longitud, las valide (rangos válidos) y responda errores públicos estables. No implementar geocodificación en el backend.
- Definir modelos de WeatherApp independientes de los DTO del proveedor.
- Implementar el caso de uso y el adapter de OpenWeather; traducir solicitudes, respuestas y errores al modelo interno. Usar español como `lang` predeterminado y admitir el parámetro opcional acordado.
- Mapear la observación actual y los IDs de alertas a tipos internos; no llamar al endpoint de detalle de alertas.
- Confirmar que el plan de OpenWeather habilita One Call 4.0 antes de depender de `/data/4.0/onecall/current`.
- Validar el formato y las unidades con respuestas HTTP controladas: el ejemplo recibido tiene temperatura `286.42` pese a `units=metric` y no es JSON válido por una coma faltante antes de `alerts`.
- Usar HTTP parametrizado y timeouts; no exponer API keys, cuerpos crudos ni detalles internos en errores de la API.

**Gate:** contrato documentado y pruebas con respuestas HTTP controladas para éxito y fallos representativos.

### 4. Proveedor secundario y resiliencia

- Implementar el adapter de Open-Meteo para el mismo contrato de consulta.
- Normalizar unidades y producir descripciones en el idioma solicitado; dejar opcionales los campos que Open-Meteo no ofrezca de forma comparable.
- Agregar `com.open-meteo:sdk` desde Maven Central en la versión estable vigente verificada durante la implementación.
- El adapter debe realizar HTTP con el cliente de la aplicación y pedir `format=flatbuffers`; el SDK solo decodifica. Validar status HTTP y cuerpo antes de pasar el buffer little-endian, size-prefixed, al `WeatherApiResponse`.
- Mapear variables usando enums/helpers del SDK y su unidad/metadatos (filtrando por altitud cuando aplique), no mediante índices fijos. Convertir timestamps Unix en segundos a instantes ISO-8601.
- Solicitar en `current=` solo variables que el endpoint actual admita y el contrato necesite; verificar las unidades. Traducir los códigos WMO de Open-Meteo a descripciones localizadas, con manejo definido para códigos desconocidos.
- Coordinar principal y secundario mediante Strategy y una política de fallback explícita.
- Configurar timeout, Circuit Breaker y reintentos limitados e independientes por proveedor; no reintentar errores de entrada ni configuración.
- Devolver error consistente si ambos proveedores fallan; nunca fabricar datos meteorológicos.

**Gate:** pruebas deterministas de fallback, Circuit Breaker, reintentos y error final; pruebas de decodificación con respuesta FlatBuffers controlada, sin llamadas externas ni credenciales reales.

Referencia técnica para el adapter: [README general del SDK](https://github.com/open-meteo/sdk/blob/main/README.md), [README Java](https://github.com/open-meteo/sdk/blob/main/java/README.md) y [documentación de Weather Forecast API](https://open-meteo.com/en/docs). El README Java consultado muestra `com.open-meteo:sdk:1.4.0`; verificar versión vigente en Maven Central antes de fijarla.

### 5. Revisión de calidad y correcciones

- Entregar cambios al flujo de revisión `/r` y resolver hallazgos bloqueantes o aceptados.
- Si se requieren cambios de implementación, volver al `dev-agent`, revisar el diff actualizado y repetir `/r`.
- El agente `05-code-reviewer-agent` es opcional: usarlo solo si se necesita su quality gate y archivo de feedback además de `/r`, evitando duplicar revisiones sin propósito.

**Gate:** sin hallazgos bloqueantes pendientes.

### 6. Diseño, implementación y ejecución de pruebas

- Invocar `06-test-flow-designer-java` con `spec.md`, `architecture-proposal.md` y el código implementado. No existe `constitution.md`; indicarle que no asuma su contenido y que registre esa ausencia.
- Revisar y aprobar el plan de pruebas; pedir cobertura trazable para requisitos, errores, fallback y resiliencia.
- Invocar `06-test-unit-dev-agent` con el plan aprobado. Respetar su alcance: implementa tests, pero no ejecuta Maven ni modifica producción.
- Ejecutar la suite y las pruebas de integración HTTP controladas; corregir fallos y revisar cambios de producción con el `dev-agent` si hace falta.

**Gate:** pruebas reproducibles pasan sin llamadas reales ni secretos.

### 7. Seguridad, documentación y despliegue

- Ejecutar revisión de seguridad con `07-SQR-review-agent`, prestando atención a secretos, validación, exposición de errores y configuración externa.
- Resolver hallazgos acordados y repetir las validaciones afectadas.
- Actualizar documentación con `08-documentation-agent`, manteniendo coherencia con código y decisiones finales.
- Seleccionar hosting gratuito y comprobar límites, compatibilidad de Java, configuración de secretos, disponibilidad y condiciones. Desplegar solo después de las verificaciones anteriores.

**Salida:** backend documentado, validado y desplegado en el servicio aprobado, o registro claro de la limitación del hosting elegido.

## Flujo de agentes

1. **`04-dev-agent`:** revisar spec/arquitectura y estado del repo; verificar rama; proponer archivos, cambios y dependencias; esperar aprobación explícita; implementar por fases y validar. No hacer `git add`, commit ni push.
2. **`/r`:** revisar los cambios implementados.
3. **`04-dev-agent` de nuevo, solo si aplica:** corregir hallazgos acordados y someter el diff corregido a revisión.
4. **`06-test-flow-designer-java`:** diseñar el plan trazable sobre la implementación revisada.
5. **`06-test-unit-dev-agent`:** implementar tests a partir del plan aprobado; el usuario/equipo ejecuta las pruebas.
6. **`07-SQR-review-agent`:** revisar seguridad antes del despliegue.
7. **`08-documentation-agent`:** sincronizar documentación con el resultado final.

El `agent.ps1` del sistema de agentes contiene una ruta base distinta de `C:\Users\Diego\OneDrive\Escritorio\copilot\agents`. Usar directamente los prompts/archivos desde la ruta correcta o ajustar esa herramienta antes de invocarla; no es requisito modificarla para continuar manualmente.

## Fuera del alcance actual

- Implementar el frontend React según `frontend-spec.md` (etapa 2).
- Agregar capas de radar o mapas meteorológicos (etapa 3).
- Incorporar base de datos, usuarios, caché o historial de búsquedas.
- Comprometer un proveedor de hosting, una API de mapas o características que excedan lo disponible y permitido en los planes gratuitos.
