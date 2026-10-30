import { useCallback, useEffect, useState } from "react";
import { MapView } from "./components/MapView";
import { StatusMessage } from "./components/StatusMessage";
import { WeatherCard } from "./components/WeatherCard";
import { useWeather } from "./hooks/useWeather";
import type { Coordinates } from "./types/location";
import type { WeatherResponse } from "./types/weather";

function formatCoordinates(coordinates: Coordinates): string {
  return `${coordinates.latitude.toFixed(4)}, ${coordinates.longitude.toFixed(4)}`;
}

function hasWeatherData(weather: WeatherResponse): boolean {
  return [
    weather.observedAt,
    weather.temperatureC,
    weather.feelsLikeC,
    weather.conditionDescription,
    weather.pressureHpa,
    weather.humidityPercent,
    weather.windSpeedMps,
    weather.windDirectionDegrees,
    weather.cloudCoverPercent,
    weather.visibilityMeters,
  ].some((value) => value !== null);
}

export function App() {
  const [selection, setSelection] = useState<Coordinates | null>(null);
  const [queryCoordinates, setQueryCoordinates] = useState<Coordinates | null>(null);
  const { state: weatherState, fetchWeather, resetWeather } = useWeather(queryCoordinates);

  const handleSelectionChange = useCallback(
    (coordinates: Coordinates | null) => {
      setSelection(coordinates);
      setQueryCoordinates(null);
      resetWeather();
    },
    [resetWeather],
  );

  const handleConfirm = useCallback((coordinates: Coordinates) => {
    setSelection(coordinates);
    setQueryCoordinates(coordinates);
  }, []);

  useEffect(() => {
    if (queryCoordinates) void fetchWeather();
  }, [fetchWeather, queryCoordinates]);

  return (
    <main className="app-shell">
      <header className="app-header">
        <a className="app-brand" href="/" aria-label="Weather Conginitive Service, inicio">
          <span className="app-brand__mark" aria-hidden="true">W</span>
          <span>Weather <strong>Conginitive Service</strong></span>
        </a>
        <p className="app-header__eyebrow">Laboratorio del clima</p>
      </header>

      <section className="app-intro" aria-labelledby="page-title">
        <p className="app-intro__eyebrow">Condiciones actuales</p>
        <h1 id="page-title">El clima, en el lugar que elegís.</h1>
        <p>
          Explorá el mapa y consultá datos meteorológicos normalizados desde nuestros
          proveedores.
        </p>
      </section>

      <div className="app-layout">
        <MapView
          onConfirm={handleConfirm}
          onSelectionChange={handleSelectionChange}
          isLoading={weatherState.status === "loading"}
        />

        <section className="weather-panel" aria-labelledby="weather-panel-title">
          <div className="weather-panel__heading">
            <p className="app-intro__eyebrow">Tu consulta</p>
            <h2 id="weather-panel-title">Clima actual</h2>
            {selection && (
              <p className="weather-panel__coordinates">
                Coordenadas: {formatCoordinates(selection)}
              </p>
            )}
          </div>

          {weatherState.status === "loading" && (
            <StatusMessage
              variant="loading"
              message="Consultando las condiciones meteorológicas…"
            />
          )}
          {weatherState.status === "error" && (
            <StatusMessage
              variant="error"
              message={weatherState.message}
              onRetry={() => void fetchWeather()}
            />
          )}
          {weatherState.status === "success" && (
            hasWeatherData(weatherState.data) ? (
              <WeatherCard weather={weatherState.data} />
            ) : (
              <StatusMessage
                variant="empty"
                message="El servicio no encontró datos meteorológicos para esta ubicación."
              />
            )
          )}
          {weatherState.status === "idle" && (
            <StatusMessage
              variant="empty"
              message={
                selection
                  ? "Confirmá la ubicación para consultar el clima."
                  : "Elegí un punto en el mapa o usá las coordenadas para comenzar."
              }
            />
          )}

          <p className="weather-panel__note">
            Datos actuales provistos por el servicio meteorológico. Las coordenadas se
            consultan únicamente cuando confirmás la ubicación.
          </p>
        </section>
      </div>

      <footer className="app-footer">
        <span>Weather Conginitive Service App</span>
        <span>Un laboratorio para aprender construyendo.</span>
      </footer>
    </main>
  );
}
