# WeatherApp — especificación del frontend

**Estado:** borrador funcional para afinar en conjunto  
**Etapa:** 2 — frontend  
**Ubicación del proyecto:** `WCSFront`

## 1. Objetivo

Definir una interfaz React para consultar las condiciones meteorológicas actuales de una ubicación elegida en un mapa interactivo. El frontend obtiene las coordenadas y consume la API REST de WeatherApp; la lógica de consulta a OpenWeather y el fallback a Open-Meteo permanecen en el backend.

## 2. Decisiones acordadas

- Se usará **React** para el frontend.
- Se usará **Vite** como herramienta de desarrollo y build para la SPA React.
- Se usará **npm** como gestor de paquetes y se versionará `package-lock.json`; Yarn es una alternativa de gestor de paquetes, no un reemplazo de Vite, y no hace falta añadirlo sin una necesidad concreta.
- `WCSFront` será el proyecto/carpeta de trabajo del frontend y el lugar previsto para alojarlo.
- El mapa interactivo será el mecanismo principal para elegir una ubicación. También se acordaron la búsqueda por nombre de lugar y la opción de usar la ubicación actual del dispositivo.
- La primera versión mostrará **condiciones actuales**. OpenWeather seguirá como proveedor principal y Open-Meteo como fallback en el backend.
- El pronóstico horario, la probabilidad de lluvia y otras capacidades adicionales se desarrollarán en una etapa posterior.
- El mapa inicial sirve para seleccionar una ubicación; no incluye capas meteorológicas, radar ni visualizaciones de pronóstico.
- La dirección visual será meteorológica, con jerarquía visual clara para la condición, temperatura y demás datos.
- El diseño y la implementación seguirán el enfoque **mobile-first**: primero se resolverán los flujos y la presentación en pantallas pequeñas y luego se ampliarán progresivamente para tablet y escritorio.

## 3. Alcance funcional

### Incluye

- Elegir una ubicación haciendo clic en el mapa y mostrando/moviendo un marcador.
- Buscar una ciudad o lugar y centrar el mapa en el resultado, una vez elegido el servicio de geocodificación.
- Solicitar permiso del navegador para centrar el mapa en la ubicación actual; si no se concede o falla, las demás formas de selección deben seguir disponibles.
- Consultar y mostrar las condiciones actuales devueltas por el backend.
- Mostrar estados de carga, validación, ausencia de resultados y errores de consulta de forma comprensible.
- Diseño adaptable a pantallas móviles y de escritorio, y controles utilizables con teclado y tecnologías de asistencia.
- Diseño mobile-first: contenido, mapa, panel meteorológico y controles deben funcionar en una pantalla móvil estrecha antes de aplicar mejoras para viewports mayores.

### Fuera de alcance de esta versión

- Pronóstico horario/diario, probabilidad de precipitación y alertas.
- Capas meteorológicas o mapas de radar.
- Geocodificación propia en el backend.
- Cuentas, persistencia, favoritos o historial de búsquedas.
- Elección manual del proveedor meteorológico.

## 4. Integración con el backend

El backend actual expone:

```http
GET /api/weather?latitude={latitude}&longitude={longitude}&lang=es
```

`lang` es opcional y el backend usa español por defecto. Las coordenadas deben enviarse como parámetros de consulta; el endpoint no recibe ciudad, país ni un cuerpo JSON.

La respuesta contiene un modelo propio de WeatherApp con:

- `location`: `latitude`, `longitude` y `timezone`
- `observedAt`
- `temperatureC`, `feelsLikeC`, `conditionDescription`
- `pressureHpa`, `humidityPercent`
- `windSpeedMps`, `windDirectionDegrees`
- `cloudCoverPercent`, `visibilityMeters`

Algunos valores pueden no estar disponibles y ser nulos; la interfaz debe omitir esos datos en lugar de mostrar valores inventados o marcadores engañosos. La presentación debe conservar las unidades del contrato: °C, hPa, %, m/s, grados y metros.

La implementación debe configurar la URL base del backend por entorno, no codificar una URL de producción ni incluir credenciales de proveedores meteorológicos en el navegador. En despliegue, el origen del frontend debe estar autorizado por la configuración CORS del backend.

## 5. Flujos de uso

### Selección en el mapa

1. La persona usuaria abre la pantalla principal y ve el mapa.
2. Hace clic en una ubicación; se muestra o mueve el marcador.
3. Confirma la selección y consulta el clima para esas coordenadas.
4. La interfaz muestra el estado de carga y luego las condiciones actuales o un error recuperable.

### Búsqueda de lugar

1. La persona usuaria busca un nombre de ciudad o lugar.
2. La interfaz presenta resultados distinguibles cuando haya coincidencias ambiguas.
3. Al elegir un resultado, centra el mapa y coloca el marcador en sus coordenadas.
4. La persona usuaria confirma la ubicación y consulta el clima.

### Ubicación del dispositivo

1. La persona usuaria activa explícitamente “Usar mi ubicación”.
2. El navegador solicita permiso; ante éxito, la interfaz centra el mapa y coloca el marcador.
3. La consulta se inicia según la interacción final que se acuerde para las selecciones.
4. Si se deniega el permiso o no se obtiene la posición, se informa el motivo de forma clara y se mantienen disponibles el mapa y la búsqueda.

## 6. Requisitos de experiencia y presentación

- La condición actual y la temperatura deben tener la mayor jerarquía visual.
- Los demás campos disponibles deben presentarse con etiquetas e iconografía comprensibles, no como nombres de propiedades JSON.
- Debe mostrarse la ubicación seleccionada y la hora de observación; el formato horario debe respetar `location.timezone`.
- La interfaz debe distinguir entre seleccionar una ubicación y cargar sus datos meteorológicos.
- Los errores de coordenadas o del proveedor deben mostrarse sin stack traces, cuerpos crudos ni mensajes internos.
- Los resultados de búsqueda, el marcador y el panel meteorológico deben mantenerse coherentes cuando la ubicación cambie.
- El mapa debe conservar visible la atribución requerida por el proveedor cartográfico y por sus datos.

## 7. Requisitos no funcionales

- No incluir API keys ni secretos de OpenWeather/Open-Meteo en recursos enviados al navegador.
- Manejar respuestas HTTP no exitosas y errores de red sin dejar la interfaz en estado de carga permanente.
- Evitar consultas redundantes mientras la persona usuaria mueve el marcador o explora el mapa.
- Implementar controles accesibles, foco visible, etiquetas y alternativas textuales a la información transmitida solo por color.
- Verificar las condiciones de uso, cuotas, cobertura y atribución de los servicios de mapa y geocodificación antes de seleccionarlos.

## 8. Patrones de frontend y diseño

- **Organización por funcionalidad:** agrupar código relacionado con selección de ubicación, consulta meteorológica y componentes compartidos, evitando separar todo únicamente por tipo técnico.
- **Adapter para el cliente API:** concentrar la comunicación HTTP con el backend y la conversión de respuestas/errores a tipos y errores entendidos por la interfaz. Los componentes no deben depender directamente de detalles de transporte.
- **Hooks para comportamiento reutilizable:** encapsular estados y operaciones con hooks enfocados, por ejemplo, consulta meteorológica y geolocalización; evitar duplicar la misma lógica en varios componentes.
- **Estados explícitos de consulta:** representar claramente selección pendiente, listo para consultar, carga, resultado y error, manteniendo sincronizados marcador, ubicación elegida y datos presentados. Cancelar o ignorar respuestas obsoletas cuando la ubicación cambie durante una solicitud.
- **Composición de componentes:** dividir la interfaz en componentes enfocados y componibles —por ejemplo, selector de ubicación, resumen del clima y detalles— sin imponer una separación rígida que no aporte valor.
- **Mobile-first:** diseñar primero la interacción y la presentación para pantallas pequeñas y ampliarlas progresivamente a tablet y escritorio.
- No incorporar Redux ni otra librería de estado global mientras el estado local y los hooks de React sean suficientes. Reevaluarlo solo si aparece una necesidad transversal concreta.

## 9. Criterios de aceptación iniciales

- Una ubicación seleccionada por mapa, búsqueda o geolocalización puede convertirse en una consulta válida al endpoint actual del backend.
- La pantalla presenta las condiciones actuales disponibles con sus unidades normalizadas, sin inventar campos ausentes.
- Los estados de carga, falta de resultados, permiso denegado, error de red y error del backend son comprensibles y recuperables.
- La búsqueda y la ubicación del dispositivo no impiden elegir manualmente un punto en el mapa.
- El frontend funciona en móvil y escritorio y no expone secretos de los proveedores.
- La interfaz se implementa mobile-first y se adapta progresivamente a escritorio sin perder funciones ni legibilidad.
- El mapa no presenta capas meteorológicas ni pronóstico como si fueran parte de esta entrega.
- Los componentes presentan estados de consulta coherentes y no reemplazan con resultados obsoletos la ubicación seleccionada más recientemente.

## 10. Decisiones pendientes para cerrar el diseño y la implementación

1. Elegir el proveedor de teselas/cartografía y verificar sus condiciones de uso y atribución. React-Leaflet y OpenStreetMap son candidatos, no decisiones técnicas finales.
2. Elegir un servicio de geocodificación para la búsqueda, comprobando cobertura en Argentina, cuotas, privacidad, licencia y atribución.
3. Definir el centro y zoom iniciales del mapa.
4. Decidir si cada selección consulta automáticamente o si la persona usuaria confirma con un botón. Se recomienda confirmación explícita para evitar llamadas innecesarias mientras explora el mapa.
5. Precisar la dirección visual meteorológica (paleta, tipografía, iconos, composición y marca) a partir del diseño que se elaborará en conjunto.
6. Definir la estrategia de despliegue y URL/orígenes permitidos para la integración con el backend.
7. Acordar cómo se atribuirá la fuente de datos meteorológicos cuando el backend use el proveedor secundario; puede requerir que el backend identifique el proveedor utilizado o mostrar atribuciones apropiadas para ambos.

## 11. Nota de arquitectura

La biblioteca de mapas, el proveedor de teselas y el servicio de geocodificación son decisiones separadas. Un mapa base no implica incorporar mapas meteorológicos. Si se adopta OpenStreetMap, debe seleccionarse un servicio de teselas apropiado y cumplir su política de uso y atribución; los datos de Open-Meteo también requieren atribución bajo sus condiciones de licencia.
