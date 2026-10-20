import type { WeatherLocation } from "./location";

export interface WeatherResponse {
  location: WeatherLocation | null;
  observedAt: string | null;
  temperatureC: number | null;
  feelsLikeC: number | null;
  conditionDescription: string | null;
  pressureHpa: number | null;
  humidityPercent: number | null;
  windSpeedMps: number | null;
  windDirectionDegrees: number | null;
  cloudCoverPercent: number | null;
  visibilityMeters: number | null;
}
