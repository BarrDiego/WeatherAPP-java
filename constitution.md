# Constitution — WeatherApp Testing

## Propósito

Esta constitución define las reglas para la validación del backend de WeatherApp durante la etapa 1. Tiene como objetivo mantener pruebas deterministas, mantenibles y alineadas con el diseño actual del proyecto.

## Principios generales

### 1. No depender de servicios externos reales

Las pruebas unitarias no deben invocar OpenWeather, Open-Meteo ni ninguna API externa real.

- Los proveedores deben mockearse o reemplazarse por dobles de prueba.
- No se deben incluir tokens, claves ni secretos reales en tests.
- Las pruebas deben ejecutarse en entornos aislados y reproducibles.

### 2. Probar comportamiento, no implementación

Las pruebas deben validar lo que la aplicación hace, no cómo lo hace internamente.

- Se prueba el resultado visible del caso de uso y del controller.
- No se prueba la implementación exacta de cada línea.
- No se validan detalles internos irrelevantes para la persona que usa la API.

### 3. Determinismo sobre datos simulados

Toda prueba debe usar datos controlados y predecibles.

- Coordenadas válidas e inválidas.
- Respuestas externas simuladas con datos mínimos necesarios.
- Casos de error expresos: timeout, respuesta vacía, proveedor no disponible, coordenadas fuera de rango.

### 4. Mantener la API pública estable

La API debe responder con contratos claros y sin filtrar detalles sensibles.

- Errores de validación deben ser públicos y consistentes.
- No se exponen claves, llamadas internas ni stack traces.
- La salida pública debe estar normalizada a un formato propio de WeatherApp.

## Regla de diseño para tests

### 5. Unit tests: clases pequeñas, foco preciso

Las pruebas unitarias deben cubrir:

- validación de entrada,
- caso feliz,
- error de coordenada inválida,
- fallback a proveedor secundario,
- fallos recuperables y no recuperables,
- respuesta final cuando ambos proveedores fallan.

Cada test debe centrarse en un solo comportamiento.

## 6. Estructura de test recomendada

Se usará JUnit 5 + Mockito.

Patrón recomendado:

- Arrange: preparar datos de entrada y mocks
- Act: ejecutar el método bajo prueba
- Assert: verificar resultado y comportamiento esperado

Ejemplo mínimo:

- entrada válida ⇒ devuelve respuesta con datos esperados
- entrada inválida ⇒ lanza o retorna error de validación
- proveedor principal falla y proveedor secundario funciona ⇒ fallback exitoso
- ambos fallan ⇒ error consistente

## 7. Cobertura mínima requerida

Se considera obligatorio cubrir al menos:

- validación de latitud y longitud
- caso de exitoso con proveedor principal
- caso de fallback con proveedor secundario
- error cuando ambos proveedores fallan
- respuesta con idioma por defecto y parámetro opcional
- validación de errores HTTP o de respuesta malformada

## 8. Reglas de nomenclatura

Los tests deben nombrarse con claridad y describir el escenario.

Ejemplos:

- shouldReturnWeatherWhenLatitudeAndLongitudeAreValid
- shouldRejectLatitudeOutOfRange
- shouldFallbackToSecondaryProviderWhenPrimaryFails
- shouldReturnBadRequestWhenCoordinatesAreMissing

## 9. Reglas de calidad

- Un test no debe depender del orden de ejecución.
- Las pruebas no deben tocar archivos reales ni entorno de producción.
- No se debe reutilizar un test para validar dos requisitos distintos.
- Si una dependencia externa se usa, debe estar completamente simulada.
- Un test debe fallar por una sola razón clara.

## 10. Criterio de aceptación para la etapa 1

La etapa puede considerarse apropiadamente validada cuando:

- los tests unitarios pasan en forma determinista,
- no hay llamadas reales a servicios meteorológicos,
- se cubren los escenarios de fallo principal, fallback y error final,
- la validación y errores públicos son reproducibles,
- la lógica de negocio queda protegida frente a cambios de proveedor.

## 11. Regla de evolución

Esta constitución puede actualizarse si la arquitectura del proyecto cambia, pero siempre deberá mantener la misma intención: pruebas confiables, aisladas, reproducibles y enfocadas en el comportamiento de la aplicación.
