# Spec: Modernización del backend WeatherApp

## 1. Contexto

WeatherApp es un proyecto personal iniciado hace aproximadamente seis años como práctica de Java. Su propósito original es consultar información meteorológica de una ubicación mediante OpenWeather. El repositorio contiene un backend Java con Spring Boot y Spring MVC, vistas JSP y un frontend experimental.

El proyecto se retomará como una modernización por etapas. Además de obtener una aplicación mantenible y extensible, se busca que el resultado permita explicar y justificar decisiones técnicas con precisión, en respuesta a feedback reciente de una postulación laboral.

## 2. Problema

La implementación actual corresponde a una etapa inicial de aprendizaje y no ofrece una base clara para evolucionar el producto ni para demostrar decisiones de diseño bien fundamentadas. El backend usa Java 8 y Spring Boot 2.4.3, junto con código y dependencias de distintas generaciones. La consulta meteorológica y la interfaz existentes no constituyen todavía una experiencia fiable y completa para extender.

Se necesita modernizar primero el backend, haciendo explícitos los requisitos, el comportamiento esperado y los motivos de las decisiones de diseño. En una segunda etapa se incorporará o renovará el frontend y se evaluarán nuevas funcionalidades meteorológicas.

## 3. Objetivo

Definir una evolución gradual de WeatherApp que modernice su backend, permita consultar información meteorológica de una ubicación mediante coordenadas y proveedores externos, y sirva como caso práctico para aplicar y explicar principios SOLID y patrones de diseño cuando aporten valor real.

La solución debe incluir pruebas unitarias y documentación breve de las alternativas de diseño consideradas, la decisión tomada, sus motivos y cómo se valida.

## 4. Estado actual

- El proyecto backend está bajo `WCSApp` y utiliza Java 8, Spring Boot 2.4.3, Maven y empaquetado WAR.
- La aplicación contiene componentes de Spring MVC, JSP y modelos para ciudad y datos meteorológicos.
- Hay un cliente para invocar OpenWeather, pero el flujo de búsqueda y la construcción/uso de la solicitud no están integrados de forma fiable.
- La configuración del cliente externo y el manejo de la respuesta requieren revisión antes de que el servicio pueda evolucionar.
- El repositorio contiene `WCSFront`, con un prototipo previo de búsqueda por ciudad que no coincide con el contrato actual de coordenadas del backend; la nueva interfaz se especifica en `frontend-spec.md`.
- No se identifica una necesidad actual de persistencia de datos.

## 5. Estado esperado

- El backend se ejecuta con Java 25 y Spring Boot 3.
- Una persona usuaria puede solicitar información meteorológica para una ubicación mediante latitud y longitud, y recibir una respuesta clara o un error comprensible cuando la entrada o el proveedor no permitan completar la consulta.
- El backend consulta OpenWeather como proveedor principal y puede recurrir a Open-Meteo como proveedor secundario ante fallos recuperables, sin exponer detalles internos innecesarios.
- La API de WeatherApp presenta un modelo propio de condiciones actuales, con unidades normalizadas y descripciones localizadas.
- Las decisiones de diseño son justificables a partir del problema, con alternativas y trade-offs documentados; los patrones no se agregan únicamente por cumplir una lista.
- La lógica relevante cuenta con pruebas unitarias y la integración con servicios externos puede validarse sin depender de llamadas reales durante las pruebas unitarias.
- El frontend se abordará en una etapa posterior, con una experiencia coherente con las capacidades del backend.

## 6. Alcance

### Incluye

- **Etapa 1 — backend:** modernizar el backend del proyecto para Java 25 y Spring Boot 3.
- Definir y ofrecer una capacidad de consulta meteorológica por coordenadas mediante OpenWeather como proveedor principal y Open-Meteo como proveedor secundario.
- Mapear internamente los identificadores de alertas que entregue OpenWeather, sin consultar sus detalles ni darles uso funcional en esta etapa.
- Definir validación de entradas y respuestas y un manejo comprensible de errores de consulta.
- Aplicar principios SOLID y patrones de diseño cuando resuelvan necesidades identificadas. Evaluar, como ejercicio explícito, una solución con condicionales frente a Strategy para un comportamiento con alternativas intercambiables.
- Incorporar pruebas unitarias y documentar brevemente el problema de diseño, alternativas, decisión, justificación y validación.
- **Etapa 2 — frontend:** implementar la interfaz React especificada en `frontend-spec.md`, con un mapa interactivo para seleccionar ubicación, búsqueda de lugares y opción de ubicación del dispositivo.
- **Etapa 3 — mapas meteorológicos:** agregar mapas de radar de precipitaciones u otras capas que estén disponibles bajo los planes gratuitos de las API seleccionadas.
- Mantener la persistencia fuera del alcance inicial, salvo que una necesidad funcional futura la justifique.

### Excluye

- Implementar ahora el frontend de la etapa 2.
- Implementar el frontend o los mapas durante la etapa 1.
- Comprometerse con un producto/API, plan comercial o parámetros operativos antes de verificar las capacidades y condiciones de uso vigentes de ambos proveedores.
- Incorporar una base de datos sin un requerimiento que la justifique.
- Reescribir funcionalidades ajenas a WeatherApp o convertir el proyecto en una plataforma meteorológica completa.

## 7. Requerimientos funcionales

- **RF1:** El backend debe permitir solicitar las condiciones meteorológicas actuales mediante latitud y longitud provistas en la solicitud. La obtención o selección de esas coordenadas corresponde al frontend.
- **RF2:** La respuesta debe presentar la información meteorológica acordada en términos comprensibles y no exigir que la persona usuaria interprete directamente la respuesta cruda del proveedor.
- **RF3:** El backend debe informar de manera consistente cuando las coordenadas no sean válidas o el proveedor no permita completar la consulta para esa ubicación.
- **RF4:** La configuración necesaria para comunicarse con el proveedor debe poder suministrarse fuera del código fuente y no exponerse en las respuestas de la aplicación.
- **RF5:** La solución debe permitir evaluar una alternativa basada en Strategy frente a una implementación con condicionales para un comportamiento con más de una opción válida. La documentación debe explicar si Strategy resulta apropiado y por qué; no se exige aplicar el patrón si no existe una necesidad real.
- **RF6:** La etapa 2 implementará un frontend React con mapa interactivo para seleccionar ubicaciones y consultar las condiciones actuales. El mapa base y la búsqueda de lugares requieren seleccionar servicios y verificar sus condiciones. Las capas meteorológicas y el pronóstico quedan para etapas posteriores.
- **RF7:** Ante fallos recuperables del proveedor principal OpenWeather, el backend debe intentar la consulta con Open-Meteo como proveedor secundario configurado. Si ambos proveedores no permiten completar la consulta, debe responder con un error comprensible y consistente, sin exponer detalles internos ni inventar información meteorológica.
- **RF8:** La API debe devolver descripciones meteorológicas en español por defecto y permitir solicitar otro idioma mediante un parámetro opcional cuando sea compatible. Cada adaptador debe producir descripciones equivalentes en el idioma solicitado.
- **RF9:** El adaptador de OpenWeather debe mapear los identificadores de alertas presentes en la respuesta a una representación interna. En esta etapa no debe consultar el endpoint de detalle de alertas ni exponerlas como funcionalidad de la API.
- **RF10:** El adaptador de Open-Meteo debe usar el SDK Java oficial para decodificar respuestas meteorológicas FlatBuffers. Las llamadas HTTP y la política de resiliencia permanecen bajo control de la aplicación.

## 8. Requerimientos no funcionales

- **RNF1:** El backend debe compilar y ejecutarse con Java 25 y Spring Boot 3.
- **RNF2:** La lógica de negocio debe poder verificarse mediante pruebas unitarias reproducibles que no requieran disponibilidad de servicios meteorológicos externos.
- **RNF3:** Las pruebas deben comprobar el comportamiento ante respuestas y fallos representativos del proveedor, sin depender de credenciales reales.
- **RNF4:** Las credenciales y otros valores sensibles de integración no deben quedar expuestos en el repositorio ni en respuestas visibles para la persona usuaria.
- **RNF5:** Las decisiones de diseño relevantes deben explicarse con suficiente precisión para que otra persona pueda comprender el problema, las alternativas, los trade-offs, la decisión y cómo se valida.
- **RNF6:** La solución debe evitar complejidad y dependencias innecesarias; no debe introducir persistencia mientras no haya una necesidad funcional concreta.
- **RNF7:** Cada integración externa debe tener límites de tiempo, un Circuit Breaker independiente y una política de reintentos acotada a fallos transitorios. La política no debe reintentar indefinidamente ni reintentar errores de entrada o configuración, y debe respetar los límites de uso de cada proveedor.
- **RNF8:** La aplicación debe poder desplegarse en un servicio online gratuito, sujeto a los límites y condiciones del servicio que se seleccione.

## 9. Casos edge

- La latitud o longitud están ausentes, fuera de rango o no son numéricas.
- El proveedor no dispone de datos para las coordenadas solicitadas.
- El proveedor responde con un error, una respuesta incompleta o un formato no esperado.
- La solicitud al proveedor no puede completarse por indisponibilidad, límite de uso o demora.
- Falla el proveedor principal y la consulta se completa con el proveedor secundario.
- Fallan ambos proveedores o ambos alcanzan sus límites; se devuelve un error controlado.
- La configuración o credencial requerida por el proveedor falta o no es válida.
- Se evalúa cambiar de proveedor o añadir una fuente de datos, y la solución debe mantener una respuesta coherente para la aplicación.
- Una futura solicitud de mapas depende de cobertura, formato, licencia o límites diferentes a los de la consulta meteorológica básica.

## 10. Criterios de aceptación

- [ ] El backend puede compilarse y ejecutarse usando Java 25 y Spring Boot 3.
- [ ] Una consulta válida por coordenadas devuelve la información meteorológica acordada en un formato definido por la API de WeatherApp.
- [ ] Las coordenadas inválidas y los fallos del proveedor producen errores consistentes y comprensibles, sin filtrar credenciales ni detalles internos sensibles.
- [ ] La configuración del proveedor no requiere incluir credenciales reales en el código fuente.
- [ ] Hay pruebas unitarias para el comportamiento principal, la validación y los escenarios de error; se ejecutan sin llamar al servicio externo real.
- [ ] La integración puede probarse mediante respuestas controladas del proveedor y contempla al menos un caso exitoso y fallos representativos.
- [ ] Si falla el proveedor principal, el backend puede usar el secundario configurado; si ambos fallan, devuelve un error controlado. Las pruebas no dependen de servicios ni credenciales reales.
- [ ] La respuesta pública de condiciones actuales usa unidades normalizadas y descripciones en español por defecto, con idioma opcional.
- [ ] Los reintentos son limitados y solo aplican a fallos transitorios; Circuit Breaker y reintentos se prueban por proveedor.
- [ ] Existe una explicación breve que compare condicionales y Strategy en un caso pertinente, describa las alternativas y sus trade-offs, justifique la decisión y explique cómo se valida.
- [ ] La selección del proveedor y los datos meteorológicos que forman la respuesta quedan documentados antes de dar por terminada la primera etapa.
- [x] Se acordó el alcance funcional inicial de la etapa 2 (React, mapa interactivo para elegir ubicación, búsqueda de lugares y opción de geolocalización); el diseño visual y la selección de servicios externos se detallan en `frontend-spec.md` antes de implementar.
- [ ] El despliegue online se realiza en un servicio gratuito seleccionado y compatible con los componentes de la aplicación.

## 11. Riesgos

- **Risk1:** Java 25 y Spring Boot 3 pueden requerir actualizar o retirar dependencias y componentes heredados. Mitigación: identificar incompatibilidades como parte de la modernización y validar la compilación y ejecución con las versiones objetivo.
- **Risk2:** Los límites, disponibilidad, cobertura, coste o condiciones de uso de OpenWeather, Open-Meteo o el proveedor de mapas pueden restringir consultas o mapas. Mitigación: verificar sus capacidades y condiciones antes de comprometer funcionalidades.
- **Risk3:** Un diseño que fuerce Strategy u otros patrones sin variantes reales puede añadir complejidad en lugar de mejorar el mantenimiento. Mitigación: comparar explícitamente el diseño con condicionales y justificar el patrón según el problema presente.
- **Risk4:** El alcance de “datos del clima” y de las funcionalidades posteriores todavía es amplio. Mitigación: acordar los campos de la consulta inicial y detallar por separado las funcionalidades de la segunda etapa.
- **Risk5:** La configuración sensible heredada puede estar presente en el código actual. Mitigación: tratar las credenciales como información que debe revisarse y gestionarse fuera del código, y evitar reutilizar o publicar valores expuestos.
- **Risk6:** La migración y refactorización total pueden crecer más allá del valor de una primera etapa. Mitigación: priorizar un backend funcional de consulta meteorológica y posponer frontend y ampliaciones hasta cerrar esa entrega.
- **Risk7:** Open-Meteo puede tener límites, cobertura, coste o condiciones diferentes a OpenWeather. Mitigación: verificar las condiciones vigentes de ambos antes de activar el fallback; si ambos fallan, devolver un error controlado.
- **Risk8:** La disponibilidad de alojamiento gratuito y sus límites pueden afectar la operación del backend o frontend desplegado. Mitigación: seleccionar el proveedor de hosting y verificar sus límites antes del despliegue.

## 12. Observaciones

- **Supuesto:** La primera entrega se limita al backend; la interfaz y las funcionalidades extendidas pertenecen a una segunda etapa.
- **Supuesto:** No se requiere base de datos para la primera entrega, ya que no se indicó persistencia de usuarios, búsquedas u otra información.
- **Decisión:** OpenWeather será el proveedor meteorológico principal y Open-Meteo el proveedor secundario para resiliencia/fallback. Antes de implementar, se verificarán las API concretas, cobertura, límites y condiciones de uso vigentes.
- **Decisión:** React es el framework acordado para la etapa 2. El frontend vivirá en `WCSFront` y ofrecerá un mapa interactivo, búsqueda de lugares y uso opcional de la ubicación del dispositivo. React-Leaflet y OpenStreetMap (OSM) siguen siendo candidatos sujetos a verificar condiciones de uso y atribución. El mapa de esta etapa selecciona ubicaciones; no es un mapa meteorológico.
- **Decisión:** La etapa 3 incorporará mapas de radar de precipitaciones o las capas meteorológicas que estén disponibles dentro de los planes gratuitos de las API seleccionadas.
- **Objetivo de despliegue:** desplegar la aplicación en un servicio online gratuito, sujeto a verificar disponibilidad y límites adecuados. El proveedor de hosting está pendiente de selección.
- **Decisión:** La consulta inicial corresponde a condiciones meteorológicas actuales. La probabilidad de precipitación queda fuera de este modelo porque requiere datos de pronóstico.
- **Decisión:** El modelo público incluirá ubicación (latitud, longitud y zona horaria), hora de observación, temperatura y sensación térmica en °C, descripción de condiciones, presión en hPa, humedad en porcentaje, velocidad del viento en m/s, dirección del viento en grados, nubosidad en porcentaje y visibilidad en metros. Los campos que un proveedor no ofrezca podrán ser opcionales.
- **Decisión:** Las descripciones se solicitan en español por defecto; se permitirá un parámetro opcional de idioma por consulta y cada adaptador normalizará/localizará la descripción según sus capacidades.
- **Decisión:** Los IDs de alertas de OpenWeather se mapearán internamente, pero no se consultará el endpoint de detalle ni se usarán funcionalmente en esta etapa.
- **Decisión:** El adapter Open-Meteo usará el SDK Java oficial `com.open-meteo:sdk` para decodificar respuestas de Weather Forecast API solicitadas en formato FlatBuffers. El SDK no reemplaza al cliente HTTP de la aplicación.
- **Decisión:** El backend recibe latitud y longitud; la selección/obtención de coordenadas y la resolución de nombres de lugares son responsabilidad del frontend.
- **Pendiente:** Seleccionar las API/productos concretos de OpenWeather y Open-Meteo para la consulta inicial y verificar compatibilidad de datos, cobertura, cuotas y condiciones de uso.
- **Pendiente:** Confirmar y fijar la versión vigente del SDK Java `com.open-meteo:sdk` al implementar.
- **Pendiente:** Definir el orden de fallback y los errores que activan el cambio entre OpenWeather y Open-Meteo.
- **Pendiente:** Precisar el comportamiento observable esperado ante errores del proveedor y solicitudes repetidas.
- **Pendiente:** En una etapa futura, definir si se incorporará pronóstico con probabilidad de precipitación y qué horizonte temporal se expondrá.
- **Pendiente:** En la etapa 2, cerrar el diseño visual, elegir el proveedor de mapa y el servicio de geocodificación, definir el centro inicial y acordar cuándo se dispara la consulta meteorológica. Verificar condiciones de uso, cobertura y atribución.
- **Pendiente:** Acordar cómo se atribuye la fuente meteorológica cuando el backend completa una consulta mediante Open-Meteo como fallback.
- **Pendiente:** En la etapa 3, precisar las capas meteorológicas de radar de precipitaciones u otras disponibles en los planes gratuitos de las API elegidas, además de su cobertura, actualización y condiciones de uso.
- **Pendiente:** Seleccionar el servicio de hosting gratuito y verificar sus límites, soporte de Java 25 y requisitos de despliegue.
- **Mejora sugerida:** Mantener una nota de decisiones técnicas con el problema, alternativas, trade-offs, decisión y validación de cada decisión importante, para que el proyecto sirva como material concreto de explicación técnica.

## 13. Fases de trabajo para la etapa 1

La implementación de la primera etapa del backend se organizará en las siguientes fases:

1. **Refactorización y documentación:** migrar el backend a Java 25 y Spring Boot 3, ordenar la estructura y retirar o adaptar los componentes heredados que no correspondan al backend REST. Documentar el estado inicial, el alcance de la refactorización, las decisiones tomadas y cómo se valida la migración.
2. **Patrones y desarrollo:** implementar la consulta meteorológica REST usando Adapter para aislar cada integración externa, Strategy para intercambiar entre proveedor principal y secundario, y Circuit Breaker independiente por proveedor para limitar llamadas durante fallos persistentes. Incorporar reintentos acotados para fallos transitorios y un fallback al secundario según una política definida. Documentar las alternativas y los motivos de aplicar estos patrones.
3. **Code review:** revisar los cambios de las fases anteriores mediante `/r`, registrar hallazgos relevantes y resolver los problemas acordados antes de dar la implementación por lista para validación.
4. **Pruebas unitarias:** completar y ejecutar las pruebas unitarias con JUnit y Mockito, sin llamadas reales al proveedor ni credenciales reales. Cubrir el comportamiento principal, la validación y los escenarios representativos de error. Los tests necesarios para desarrollar y revisar cada cambio se pueden escribir antes; esta fase es la validación final de la cobertura unitaria acordada.

Las fases describen el orden de entrega y revisión, no impiden escribir pruebas durante el desarrollo. La prueba de integración del adaptador con respuestas HTTP controladas también forma parte de los criterios de aceptación de esta etapa y debe realizarse sin depender del servicio externo real.
