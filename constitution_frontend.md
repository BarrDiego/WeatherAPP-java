# Constitución del Frontend — WCSFront

## Propósito y alcance

Este documento establece las reglas de arquitectura, implementación y experiencia de usuario para el frontend de WeatherApp en `WCSFront`. Su objetivo es mantener una aplicación React clara, accesible, adaptable y desacoplada de los proveedores meteorológicos externos.

La primera versión presenta condiciones meteorológicas actuales para una ubicación seleccionada. El pronóstico, las capas meteorológicas, las alertas y la elección manual del proveedor quedan fuera de alcance hasta que se acuerde su incorporación.

## 1. Principios de arquitectura

### 1.1 Mobile-first obligatorio

Todo flujo y layout debe diseñarse primero para viewports estrechos. La selección de ubicación, los controles, los estados de consulta y la tarjeta meteorológica deben ser utilizables en móvil antes de aplicar mejoras progresivas para tablet y escritorio.

- Usar unidades y tamaños flexibles para evitar desbordes en pantallas estrechas.
- Mantener controles táctiles legibles y con áreas de interacción adecuadas.
- Las media queries deben ampliar o reorganizar la experiencia móvil, no ser necesarias para que funcione.

### 1.2 Adapter como único cliente del backend

`src/services/weatherAdapter.ts` es el único punto de contacto HTTP con el backend meteorológico.

- Los componentes y hooks de presentación no construyen URLs, ejecutan `fetch` ni interpretan códigos HTTP.
- El Adapter valida y normaliza la respuesta a los tipos de la aplicación y traduce los fallos de transporte a errores entendibles por la interfaz.
- La URL base se configura por entorno mediante `VITE_API_BASE_URL`; no se codifican URLs de producción.

### 1.3 Resiliencia y control asíncrono

Las consultas deben mantener estados explícitos: `idle`, `loading`, `success` y `error`.

- Las peticiones compatibles con cancelación reciben un `AbortSignal`.
- Al cambiar la ubicación, cancelar o invalidar la consulta anterior para impedir que una respuesta obsoleta reemplace el resultado vigente.
- Los errores deben finalizar el estado de carga y ofrecer una recuperación comprensible; no se muestran stack traces ni detalles internos.
- La selección de una ubicación se mantiene separada de la confirmación de la consulta.

### 1.4 Cero secretos en el cliente

Toda consulta a OpenWeather, Open-Meteo u otros proveedores meteorológicos externos pasa por el backend Java.

- No incluir claves API, credenciales ni secretos de proveedores en código, configuración versionada o recursos enviados al navegador.
- Las variables `VITE_*` son públicas en el bundle y nunca deben contener secretos.
- El backend conserva la responsabilidad de elegir el proveedor principal y aplicar el fallback correspondiente.

### 1.5 Organización y estado

Organizar el código alrededor de funcionalidades y responsabilidades cohesivas. Usar componentes desacoplados para la presentación y custom hooks para comportamiento reutilizable.

- Preferir estado local de React y custom hooks.
- No incorporar Redux, Zustand ni otra librería de estado global mientras el estado local sea suficiente.
- Reevaluar esta decisión únicamente ante una necesidad transversal concreta.

## 2. Manejo de datos y contrato

### 2.1 Fidelidad al contrato del backend

El frontend consulta exclusivamente el contrato actual:

```http
GET /api/weather?latitude={latitude}&longitude={longitude}&lang=es
```

Las coordenadas viajan como parámetros de consulta; no se envía un cuerpo JSON. El modelo de respuesta incluye `location` (`latitude`, `longitude`, `timezone`), `observedAt`, `temperatureC`, `feelsLikeC`, `conditionDescription`, `pressureHpa`, `humidityPercent`, `windSpeedMps`, `windDirectionDegrees`, `cloudCoverPercent` y `visibilityMeters`.

Los tipos TypeScript deben reflejar las unidades y la posibilidad de valores nulos que establece el backend. Si el contrato cambia, actualizar conjuntamente los tipos y el Adapter.

### 2.2 Inmutabilidad y valores nulos

Los datos recibidos no deben modificarse para fabricar una presentación aparentemente completa.

- Si un campo opcional llega como `null`, se omite de la interfaz.
- Está prohibido inventar, mockear o sustituir datos de producción ausentes por valores por defecto engañosos.
- Mantener las unidades del contrato: °C, hPa, %, m/s, grados y metros.

### 2.3 Formato horario

La hora de observación se presenta respetando `location.timezone`. Si la fecha o la zona horaria falta o no puede interpretarse, se omite la hora; no se asume una zona local que pueda inducir a error.

## 3. Reglas de componentes y experiencia de usuario

### 3.1 Confirmación explícita

Seleccionar o explorar ubicaciones en el mapa no dispara consultas meteorológicas. La consulta requiere una acción explícita, como el botón **“Consultar clima”**, para evitar llamadas innecesarias.

### 3.2 Geolocalización segura

`useGeolocation` solicita la ubicación solo como respuesta a una acción explícita de la persona usuaria.

- Obtener la posición actualiza la selección del mapa, pero no consulta automáticamente el backend.
- Si el permiso se deniega o el dispositivo no puede determinar la ubicación, informar el motivo y permitir continuar usando el mapa u otros medios disponibles.
- La geolocalización debe funcionar en un contexto seguro compatible con las restricciones del navegador.

### 3.3 Estados y mensajes recuperables

La interfaz distingue ubicación no seleccionada, carga, éxito, error, ausencia de datos y problemas de geolocalización. `StatusMessage` utiliza roles y anuncios ARIA apropiados para cada estado y ofrece una acción de reintento cuando el error sea recuperable.

Los mensajes son claros, accionables y no exponen cuerpos crudos de respuesta, stack traces ni detalles internos del backend o de los proveedores.

### 3.4 Presentación meteorológica

`WeatherCard` da prioridad visual a la temperatura y a la descripción de la condición. Los demás campos disponibles se muestran con etiquetas comprensibles y unidades fieles al contrato; los valores nulos se omiten.

### 3.5 Accesibilidad

- Los controles deben tener etiquetas accesibles, foco visible y uso por teclado cuando corresponda.
- Los mensajes de estado deben anunciarse con semántica ARIA adecuada.
- La información no debe comunicarse únicamente mediante color.
- El mapa debe proporcionar controles e indicaciones accesibles, y conservar visible la atribución cartográfica requerida.

## 4. Stack tecnológico y decisiones acordadas

- **React + Vite + TypeScript** para la aplicación de página única.
- **npm** como gestor de paquetes; versionar `package-lock.json` para instalaciones reproducibles.
- **React-Leaflet + Leaflet** para el selector de ubicación.
- **OpenStreetMap** como fuente cartográfica inicial, con la atribución requerida visible. Respetar las políticas vigentes del servicio de teselas elegido.
- `VITE_API_BASE_URL` configura el origen del backend; los secretos de proveedores no son parte del frontend.
- Sin librerías de estado global mientras React y los custom hooks cubran las necesidades.

## 5. Alcance cartográfico y funcionalidades

El mapa permite elegir una ubicación y confirmar una consulta; no representa radar, capas meteorológicas ni pronósticos.

La búsqueda por nombre de lugar y su servicio de geocodificación requieren una decisión explícita sobre proveedor, cobertura, cuotas, privacidad, licencia y atribución antes de incorporarse. La selección por mapa y la geolocalización deben seguir disponibles aunque la búsqueda no esté implementada o falle.

## 6. Regla de evolución

Toda modificación de arquitectura o contrato debe mantener estas garantías: integración con el backend mediante el Adapter, ausencia de secretos en el cliente, fidelidad a los datos recibidos, control de respuestas obsoletas, accesibilidad y funcionamiento mobile-first. Las decisiones nuevas que alteren estas reglas deben documentarse y reflejarse también en `frontend-spec.md` cuando afecten el alcance funcional.

## 7. TODO / Roadmap — próxima sesión

- [ ] Implementar e integrar `src/App.tsx`, ensamblando `MapView`, `WeatherCard`, `StatusMessage` y los custom hooks en un layout responsivo mobile-first.
- [ ] Ejecutar `npm install` localmente para instalar las dependencias y generar `package-lock.json`.
- [ ] Validar la compilación de TypeScript con `npm run build` y levantar el servidor de desarrollo con `npm run dev`.
- [ ] Realizar una prueba de integración End-to-End entre `WCSFront` y el backend Spring Boot donde se ejecuta `wcsAgent`.
