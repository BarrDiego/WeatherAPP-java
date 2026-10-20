import type { WeatherLocation } from "../types/location";
import type { WeatherResponse } from "../types/weather";

export class WeatherApiError extends Error {
  readonly status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.name = "WeatherApiError";
    this.status = status;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function nullableNumber(value: unknown, field: string): number | null {
  if (value === null) return null;
  if (typeof value === "number" && Number.isFinite(value)) return value;
  throw new WeatherApiError(`La respuesta del servicio contiene un campo inválido: ${field}.`);
}

function nullableString(value: unknown, field: string): string | null {
  if (value === null) return null;
  if (typeof value === "string") return value;
  throw new WeatherApiError(`La respuesta del servicio contiene un campo inválido: ${field}.`);
}

function parseLocation(value: unknown): WeatherLocation | null {
  if (value === null) return null;
  if (!isRecord(value)) {
    throw new WeatherApiError("La respuesta del servicio contiene una ubicación inválida.");
  }

  return {
    latitude: nullableNumber(value.latitude, "location.latitude"),
    longitude: nullableNumber(value.longitude, "location.longitude"),
    timezone: nullableString(value.timezone, "location.timezone"),
  };
}

function parseWeatherResponse(value: unknown): WeatherResponse {
  if (!isRecord(value)) {
    throw new WeatherApiError("El servicio devolvió una respuesta meteorológica inválida.");
  }

  return {
    location: parseLocation(value.location),
    observedAt: nullableString(value.observedAt, "observedAt"),
    temperatureC: nullableNumber(value.temperatureC, "temperatureC"),
    feelsLikeC: nullableNumber(value.feelsLikeC, "feelsLikeC"),
    conditionDescription: nullableString(value.conditionDescription, "conditionDescription"),
    pressureHpa: nullableNumber(value.pressureHpa, "pressureHpa"),
    humidityPercent: nullableNumber(value.humidityPercent, "humidityPercent"),
    windSpeedMps: nullableNumber(value.windSpeedMps, "windSpeedMps"),
    windDirectionDegrees: nullableNumber(value.windDirectionDegrees, "windDirectionDegrees"),
    cloudCoverPercent: nullableNumber(value.cloudCoverPercent, "cloudCoverPercent"),
    visibilityMeters: nullableNumber(value.visibilityMeters, "visibilityMeters"),
  };
}

function httpError(status: number): WeatherApiError {
  if (status === 400) {
    return new WeatherApiError(
      "Las coordenadas no son válidas. Revisá la ubicación seleccionada.",
      status,
    );
  }
  if (status === 404) {
    return new WeatherApiError("No se encontraron datos para esa ubicación.", status);
  }
  if (status >= 500) {
    return new WeatherApiError(
      "El servicio meteorológico no está disponible en este momento. Intentá nuevamente más tarde.",
      status,
    );
  }
  return new WeatherApiError(
    "No se pudo completar la consulta meteorológica. Intentá nuevamente.",
    status,
  );
}

export async function getWeather(
  latitude: number,
  longitude: number,
  signal?: AbortSignal,
): Promise<WeatherResponse> {
  if (
    !Number.isFinite(latitude) ||
    latitude < -90 ||
    latitude > 90 ||
    !Number.isFinite(longitude) ||
    longitude < -180 ||
    longitude > 180
  ) {
    throw new WeatherApiError("Las coordenadas no son válidas.");
  }

  const baseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? "";
  const endpoint = `${baseUrl.replace(/\/+$/, "")}/api/weather`;
  const query = new URLSearchParams({
    latitude: String(latitude),
    longitude: String(longitude),
    lang: "es",
  });

  let response: Response;
  try {
    response = await fetch(`${endpoint}?${query.toString()}`, { signal });
  } catch (error) {
    if (signal?.aborted) throw error;
    throw new WeatherApiError(
      "No se pudo conectar con el servicio meteorológico. Revisá tu conexión e intentá nuevamente.",
    );
  }

  if (!response.ok) {
    throw httpError(response.status);
  }

  let payload: unknown;
  try {
    payload = await response.json();
  } catch {
    throw new WeatherApiError("El servicio devolvió una respuesta que no se pudo leer.");
  }

  return parseWeatherResponse(payload);
}
