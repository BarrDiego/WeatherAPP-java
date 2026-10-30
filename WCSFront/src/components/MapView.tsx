import { useCallback, useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import {
  MapContainer,
  Marker,
  TileLayer,
  useMap,
  useMapEvents,
} from "react-leaflet";
import { divIcon } from "leaflet";
import type { LeafletMouseEvent } from "leaflet";
import { useGeolocation } from "../hooks/useGeolocation";
import {
  GeocodingApiError,
  searchAddress,
} from "../services/geocodingAdapter";
import type { GeocodingResult } from "../services/geocodingAdapter";
import type { Coordinates } from "../types/location";
import { StatusMessage } from "./StatusMessage";
import "./MapView.css";
import "leaflet/dist/leaflet.css";

interface MapViewProps {
  onConfirm: (coordinates: Coordinates) => void;
  onSelectionChange: (coordinates: Coordinates | null) => void;
  initialCoordinates?: Coordinates | null;
  isLoading?: boolean;
}

type AddressSearchState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "results"; results: GeocodingResult[] }
  | { status: "empty" }
  | { status: "error"; message: string };

const INITIAL_CENTER: [number, number] = [-38.4161, -63.6167];
const INITIAL_ZOOM = 4;
const selectedLocationIcon = divIcon({
  className: "map-view__marker-icon-wrapper",
  html: '<span class="map-view__marker-icon"></span>',
  iconSize: [24, 24],
  iconAnchor: [12, 12],
});

function isValidCoordinates(latitude: number, longitude: number): boolean {
  return (
    Number.isFinite(latitude) &&
    latitude >= -90 &&
    latitude <= 90 &&
    Number.isFinite(longitude) &&
    longitude >= -180 &&
    longitude <= 180
  );
}

function MapSelectionEvents({ onSelect }: { onSelect: (coordinates: Coordinates) => void }) {
  const handleMapClick = useCallback(
    (event: LeafletMouseEvent) => {
      onSelect({
        latitude: event.latlng.lat,
        longitude: event.latlng.lng,
      });
    },
    [onSelect],
  );

  useMapEvents({ click: handleMapClick });
  return null;
}

function MapCenter({ coordinates }: { coordinates: Coordinates | null }) {
  const map = useMap();

  useEffect(() => {
    if (coordinates) {
      map.flyTo([coordinates.latitude, coordinates.longitude], Math.max(map.getZoom(), 10), {
        duration: 0.6,
      });
    }
  }, [coordinates, map]);

  return null;
}

export function MapView({
  onConfirm,
  onSelectionChange,
  initialCoordinates = null,
  isLoading = false,
}: MapViewProps) {
  const [selection, setSelection] = useState<Coordinates | null>(initialCoordinates);
  const [latitudeInput, setLatitudeInput] = useState(
    initialCoordinates ? String(initialCoordinates.latitude) : "",
  );
  const [longitudeInput, setLongitudeInput] = useState(
    initialCoordinates ? String(initialCoordinates.longitude) : "",
  );
  const [address, setAddress] = useState("");
  const [addressSearchState, setAddressSearchState] =
    useState<AddressSearchState>({ status: "idle" });
  const addressRequest = useRef<AbortController | null>(null);
  const { state: geolocationState, requestLocation } = useGeolocation();

  const selectCoordinates = useCallback(
    (coordinates: Coordinates) => {
      setSelection(coordinates);
      setLatitudeInput(coordinates.latitude.toFixed(5));
      setLongitudeInput(coordinates.longitude.toFixed(5));
      onSelectionChange(coordinates);
    },
    [onSelectionChange],
  );

  useEffect(() => {
    if (geolocationState.status === "success") {
      selectCoordinates(geolocationState.coordinates);
    }
  }, [geolocationState, selectCoordinates]);

  useEffect(() => {
    if (initialCoordinates) {
      selectCoordinates(initialCoordinates);
    }
  }, [initialCoordinates?.latitude, initialCoordinates?.longitude, selectCoordinates]);

  useEffect(
    () => () => {
      addressRequest.current?.abort();
    },
    [],
  );

  const handleAddressSearch = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const query = address.trim();
    if (!query || addressSearchState.status === "loading") return;

    addressRequest.current?.abort();
    const controller = new AbortController();
    addressRequest.current = controller;
    setAddressSearchState({ status: "loading" });

    try {
      const results = await searchAddress(query, controller.signal);
      if (addressRequest.current !== controller) return;
      setAddressSearchState(
        results.length > 0 ? { status: "results", results } : { status: "empty" },
      );
    } catch (error) {
      if (controller.signal.aborted) return;
      setAddressSearchState({
        status: "error",
        message:
          error instanceof GeocodingApiError
            ? error.message
            : "No se pudo buscar la dirección. Intentá nuevamente.",
      });
    } finally {
      if (addressRequest.current === controller) addressRequest.current = null;
    }
  };

  const handleAddressChange = (value: string) => {
    setAddress(value);
    addressRequest.current?.abort();
    setAddressSearchState({ status: "idle" });
  };

  const handleResultSelect = (result: GeocodingResult) => {
    selectCoordinates(result.coordinates);
    setAddress(result.displayName);
    setAddressSearchState({ status: "idle" });
  };

  const updateCoordinateInputs = (
    latitudeText: string,
    longitudeText: string,
  ) => {
    setLatitudeInput(latitudeText);
    setLongitudeInput(longitudeText);

    if (latitudeText.trim() === "" || longitudeText.trim() === "") {
      setSelection(null);
      onSelectionChange(null);
      return;
    }

    const latitude = Number(latitudeText);
    const longitude = Number(longitudeText);
    if (isValidCoordinates(latitude, longitude)) {
      const coordinates = { latitude, longitude };
      setSelection(coordinates);
      onSelectionChange(coordinates);
    } else {
      setSelection(null);
      onSelectionChange(null);
    }
  };

  const handleConfirm = () => {
    if (selection) onConfirm(selection);
  };

  return (
    <section className="map-view" aria-labelledby="map-view-title">
      <div className="map-view__heading">
        <div>
          <h2 id="map-view-title">Elegí una ubicación</h2>
          <p>Tocá el mapa, arrastrá el punto o ingresá coordenadas.</p>
        </div>
        <button
          className="map-view__location-button"
          type="button"
          onClick={requestLocation}
          disabled={geolocationState.status === "loading"}
        >
          {geolocationState.status === "loading" ? "Buscando ubicación…" : "Usar mi ubicación"}
        </button>
      </div>

      {geolocationState.status === "loading" && (
        <StatusMessage variant="loading" message="Esperando la ubicación del dispositivo…" />
      )}
      {geolocationState.status === "error" && (
        <StatusMessage variant="notice" message={geolocationState.reason} />
      )}

      <form className="map-view__address-search" onSubmit={(event) => void handleAddressSearch(event)}>
        <label htmlFor="address-search">Dirección o lugar</label>
        <div className="map-view__address-controls">
          <input
            id="address-search"
            type="search"
            autoComplete="street-address"
            value={address}
            onChange={(event) => handleAddressChange(event.target.value)}
            placeholder="Ej.: Calle 12, La Plata"
            required
          />
          <button
            className="map-view__location-button"
            type="submit"
            disabled={!address.trim() || addressSearchState.status === "loading"}
          >
            {addressSearchState.status === "loading" ? "Buscando…" : "Buscar"}
          </button>
        </div>
      </form>

      {addressSearchState.status === "loading" && (
        <StatusMessage variant="loading" message="Buscando direcciones…" />
      )}
      {addressSearchState.status === "empty" && (
        <StatusMessage
          variant="notice"
          message="No encontramos coincidencias. Probá con una dirección o localidad más específica."
        />
      )}
      {addressSearchState.status === "error" && (
        <StatusMessage variant="error" message={addressSearchState.message} />
      )}
      {addressSearchState.status === "results" && (
        <ul
          className="map-view__address-results"
          aria-label="Resultados de búsqueda"
          aria-live="polite"
        >
          {addressSearchState.results.map((result) => (
            <li
              key={`${result.coordinates.latitude},${result.coordinates.longitude},${result.displayName}`}
            >
              <button type="button" onClick={() => handleResultSelect(result)}>
                {result.displayName}
              </button>
            </li>
          ))}
        </ul>
      )}
      <p className="map-view__geocoding-attribution">
        Búsqueda provista por{" "}
        <a
          href="https://nominatim.openstreetmap.org/"
          target="_blank"
          rel="noreferrer"
        >
          Nominatim
        </a>
        {" · "}
        <a
          href="https://www.openstreetmap.org/copyright"
          target="_blank"
          rel="noreferrer"
        >
          © OpenStreetMap contributors
        </a>
        . La dirección ingresada se envía a OpenStreetMap; no incluyas datos confidenciales.
      </p>

      <div className="map-view__canvas" aria-label="Mapa interactivo para elegir coordenadas">
        <MapContainer
          center={
            selection
              ? [selection.latitude, selection.longitude]
              : INITIAL_CENTER
          }
          zoom={selection ? 10 : INITIAL_ZOOM}
          scrollWheelZoom
          keyboard
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <MapSelectionEvents onSelect={selectCoordinates} />
          <MapCenter coordinates={selection} />
          {selection && (
            <Marker
              position={[selection.latitude, selection.longitude]}
              icon={selectedLocationIcon}
              eventHandlers={{
                dragend: (event) => {
                  const marker = event.target;
                  const point = marker.getLatLng();
                  selectCoordinates({ latitude: point.lat, longitude: point.lng });
                },
              }}
              draggable
              keyboard
              alt="Ubicación seleccionada; arrastrá para cambiarla"
            />
          )}
        </MapContainer>
      </div>

      <div className="map-view__coordinates">
        <label>
          Latitud
          <input
            type="number"
            inputMode="decimal"
            min="-90"
            max="90"
            step="any"
            value={latitudeInput}
            onChange={(event) =>
              updateCoordinateInputs(event.target.value, longitudeInput)
            }
            aria-label="Latitud seleccionada"
          />
        </label>
        <label>
          Longitud
          <input
            type="number"
            inputMode="decimal"
            min="-180"
            max="180"
            step="any"
            value={longitudeInput}
            onChange={(event) =>
              updateCoordinateInputs(latitudeInput, event.target.value)
            }
            aria-label="Longitud seleccionada"
          />
        </label>
      </div>

      <button
        className="map-view__confirm-button"
        type="button"
        onClick={handleConfirm}
        disabled={!selection || isLoading}
      >
        {isLoading ? "Consultando clima…" : "Consultar clima"}
      </button>
    </section>
  );
}
