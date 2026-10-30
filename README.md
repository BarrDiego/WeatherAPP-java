# Weather Conginitive Service App

Laboratorio para practicar arquitectura, patrones y desarrollo con un mapa interactivo
en React y una API meteorológica en Java 25 / Spring Boot 3.

## Arquitectura actual

- `WCSFront`: React, TypeScript, Vite y React-Leaflet. El frontend consulta el backend
  meteorológico mediante `src/services/weatherAdapter.ts` y busca direcciones con
  Nominatim al enviar el formulario, sin autocompletado.
- `WCSApp`: API REST que normaliza condiciones actuales; usa OpenWeather como proveedor
  principal y Open-Meteo como fallback.
- `spec.md`, `frontend-spec.md` y las constituciones documentan alcance y criterios.

## Ejecución local

Requisitos: Java 25 y Node.js LTS con npm.

1. Iniciar el backend desde `WCSApp`:

   ```powershell
   $env:OPENWEATHER_API_KEY = "tu-clave"
   .\mvnw.cmd spring-boot:run
   ```

   El servicio queda disponible en `http://localhost:8080`. La variable de entorno es
   opcional para arrancar, pero se necesita una clave válida para obtener datos reales
   de OpenWeather.

2. Desde otra terminal, iniciar el frontend:

   ```powershell
   Set-Location ..\WCSFront
   npm ci
   Copy-Item .env.example .env
   npm run dev
   ```

   En desarrollo, Vite reenvía `/api` al backend local en `http://localhost:8080`.
   Para usar otro destino, configurar `VITE_API_BASE_URL` en `WCSFront/.env`. La
   persona usuaria selecciona un punto en el mapa y confirma para consultar el clima.

3. Validar el bundle de producción:

   ```powershell
   npm run build
   npm run preview
   ```

## Ejecutar ambos servicios en Docker

Con Docker Desktop y Compose activos, desde la raíz del repositorio:

```powershell
docker compose up --build
```

Abrir `http://localhost:4173`; el backend queda disponible en `http://localhost:8080`.
Para una consulta real, crear un archivo `.env` en la raíz con `OPENWEATHER_API_KEY`
asignada a una clave propia. No guardar ese archivo en Git.

> Nota: actualmente los adapters meteorológicos del backend todavía devuelven datos de
> ejemplo; el contenedor permite practicar el despliegue local, pero no consultar datos
> meteorológicos reales. La búsqueda de direcciones usa el servicio público de Nominatim;
> revisá su política de uso antes de publicar la app.

## Despliegue

El frontend necesita `VITE_API_BASE_URL` configurada durante el build con la URL pública
del backend. El backend debe recibir `OPENWEATHER_API_KEY` como secreto del entorno.
Antes del primer despliegue hay que elegir los servicios de hosting y configurar CORS
para el origen público del frontend. La búsqueda de direcciones usa Nominatim para el
desarrollo local; revisá los pendientes de política y selección de proveedor en
`frontend-spec.md` antes de publicar la aplicación.
