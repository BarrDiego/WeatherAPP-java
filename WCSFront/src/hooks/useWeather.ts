import { useCallback, useEffect, useRef, useState } from "react";
import { getWeather, WeatherApiError } from "../services/weatherAdapter";
import type { Coordinates } from "../types/location";
import type { WeatherResponse } from "../types/weather";

export type WeatherState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; data: WeatherResponse }
  | { status: "error"; message: string };

interface ActiveRequest {
  id: number;
  controller: AbortController;
}

const initialState: WeatherState = { status: "idle" };

export function useWeather(location: Coordinates | null) {
  const [state, setState] = useState<WeatherState>(initialState);
  const requestId = useRef(0);
  const activeRequest = useRef<ActiveRequest | null>(null);

  useEffect(() => {
    setState(initialState);

    return () => {
      requestId.current += 1;
      activeRequest.current?.controller.abort();
      activeRequest.current = null;
    };
  }, [location?.latitude, location?.longitude]);

  const cancelWeather = useCallback(() => {
    requestId.current += 1;
    activeRequest.current?.controller.abort();
    activeRequest.current = null;
    setState(initialState);
  }, []);

  const resetWeather = useCallback(() => {
    cancelWeather();
  }, [cancelWeather]);

  const fetchWeather = useCallback(async () => {
    if (!location) {
      setState(initialState);
      return;
    }

    const id = ++requestId.current;
    activeRequest.current?.controller.abort();
    const controller = new AbortController();
    activeRequest.current = { id, controller };
    setState({ status: "loading" });

    try {
      const data = await getWeather(location.latitude, location.longitude, controller.signal);
      if (requestId.current === id) {
        setState({ status: "success", data });
      }
    } catch (error) {
      if (requestId.current !== id || controller.signal.aborted) return;

      setState({
        status: "error",
        message:
          error instanceof WeatherApiError
            ? error.message
            : "No se pudo consultar el clima. Intentá nuevamente.",
      });
    } finally {
      if (activeRequest.current?.id === id) {
        activeRequest.current = null;
      }
    }
  }, [location]);

  return { state, fetchWeather, cancelWeather, resetWeather };
}
