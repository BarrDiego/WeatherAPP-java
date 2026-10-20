import { useCallback, useEffect, useRef, useState } from "react";
import type { Coordinates } from "../types/location";

export type GeolocationState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; coordinates: Coordinates }
  | { status: "error"; reason: string };

const initialState: GeolocationState = { status: "idle" };

function geolocationErrorReason(error: GeolocationPositionError): string {
  switch (error.code) {
    case error.PERMISSION_DENIED:
      return "No se concedió permiso para acceder a la ubicación.";
    case error.POSITION_UNAVAILABLE:
      return "El dispositivo no pudo determinar la ubicación.";
    case error.TIMEOUT:
      return "La solicitud de ubicación tardó demasiado. Intentá nuevamente.";
    default:
      return "No se pudo obtener la ubicación del dispositivo.";
  }
}

export function useGeolocation() {
  const [state, setState] = useState<GeolocationState>(initialState);
  const requestId = useRef(0);

  useEffect(
    () => () => {
      requestId.current += 1;
    },
    [],
  );

  const requestLocation = useCallback(() => {
    const id = ++requestId.current;
    if (typeof navigator === "undefined" || !navigator.geolocation) {
      setState({
        status: "error",
        reason: "Este navegador no permite acceder a la ubicación.",
      });
      return;
    }

    setState({ status: "loading" });
    try {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          if (requestId.current !== id) return;
          setState({
            status: "success",
            coordinates: {
              latitude: position.coords.latitude,
              longitude: position.coords.longitude,
            },
          });
        },
        (error) => {
          if (requestId.current !== id) return;
          setState({ status: "error", reason: geolocationErrorReason(error) });
        },
        { enableHighAccuracy: false, timeout: 10000, maximumAge: 60000 },
      );
    } catch {
      if (requestId.current === id) {
        setState({
          status: "error",
          reason: "No se pudo iniciar la solicitud de ubicación.",
        });
      }
    }
  }, []);

  const resetLocation = useCallback(() => {
    requestId.current += 1;
    setState(initialState);
  }, []);

  return { state, requestLocation, resetLocation };
}
