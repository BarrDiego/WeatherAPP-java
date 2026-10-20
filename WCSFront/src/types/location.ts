export interface Coordinates {
  latitude: number;
  longitude: number;
}

export interface WeatherLocation {
  latitude: number | null;
  longitude: number | null;
  timezone: string | null;
}
