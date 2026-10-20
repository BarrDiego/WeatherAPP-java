import type { WeatherResponse } from "../types/weather";
import "./WeatherCard.css";

interface WeatherCardProps {
  weather: WeatherResponse;
}

interface WeatherDetail {
  label: string;
  value: string;
}

function formatNumber(value: number, maximumFractionDigits = 0): string {
  return new Intl.NumberFormat("es-AR", { maximumFractionDigits }).format(value);
}

function formatObservedAt(observedAt: string | null, timezone: string | null): string | null {
  if (!observedAt || !timezone) return null;

  const date = new Date(observedAt);
  if (Number.isNaN(date.getTime())) return null;

  try {
    return new Intl.DateTimeFormat("es-AR", {
      dateStyle: "medium",
      timeStyle: "short",
      timeZone: timezone,
    }).format(date);
  } catch {
    return null;
  }
}

export function WeatherCard({ weather }: WeatherCardProps) {
  const details: WeatherDetail[] = [];

  if (weather.feelsLikeC !== null) {
    details.push({ label: "Sensación térmica", value: `${formatNumber(weather.feelsLikeC, 1)} °C` });
  }
  if (weather.humidityPercent !== null) {
    details.push({ label: "Humedad", value: `${formatNumber(weather.humidityPercent)} %` });
  }
  if (weather.windSpeedMps !== null) {
    details.push({ label: "Viento", value: `${formatNumber(weather.windSpeedMps, 1)} m/s` });
  }
  if (weather.windDirectionDegrees !== null) {
    details.push({
      label: "Dirección del viento",
      value: `${formatNumber(weather.windDirectionDegrees)}°`,
    });
  }
  if (weather.pressureHpa !== null) {
    details.push({ label: "Presión", value: `${formatNumber(weather.pressureHpa)} hPa` });
  }
  if (weather.visibilityMeters !== null) {
    details.push({ label: "Visibilidad", value: `${formatNumber(weather.visibilityMeters)} m` });
  }
  if (weather.cloudCoverPercent !== null) {
    details.push({ label: "Nubosidad", value: `${formatNumber(weather.cloudCoverPercent)} %` });
  }

  const observedAt = formatObservedAt(weather.observedAt, weather.location?.timezone ?? null);
  const hasPrimaryData =
    weather.temperatureC !== null ||
    Boolean(weather.conditionDescription?.trim()) ||
    details.length > 0 ||
    observedAt !== null;

  if (!hasPrimaryData) return null;

  return (
    <article className="weather-card" aria-label="Condiciones meteorológicas actuales">
      <header className="weather-card__header">
        {weather.temperatureC !== null && (
          <p className="weather-card__temperature">
            {formatNumber(weather.temperatureC, 1)}
            <span className="weather-card__unit">°C</span>
          </p>
        )}
        {weather.conditionDescription?.trim() && (
          <h2 className="weather-card__condition">{weather.conditionDescription}</h2>
        )}
      </header>

      {observedAt && (
        <p className="weather-card__observed">
          Observado el <time dateTime={weather.observedAt ?? undefined}>{observedAt}</time>
        </p>
      )}

      {details.length > 0 && (
        <dl className="weather-card__details">
          {details.map(({ label, value }) => (
            <div className="weather-card__detail" key={label}>
              <dt>{label}</dt>
              <dd>{value}</dd>
            </div>
          ))}
        </dl>
      )}
    </article>
  );
}
