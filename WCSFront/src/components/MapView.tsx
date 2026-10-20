import { useCallback, useEffect, useState } from "react";
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
import type { Coordinates } from "../types/location";
import { StatusMessage } from "./StatusMessage";
import "./MapView.css";
import "leaflet/dist/leaflet.css";

interface MapViewProps {
  onConfirm: (coordinates: Coordinates) => void;
  initialCoordinates?: Coordinates | null;
  isLoading?: boolean;
}

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
  const { state: geolocationState, requestLocation } = useGeolocation();

  const selectCoordinates = useCallback((coordinates: Coordinates) => {
    setSelection(coordinates);
    setLatitudeInput(coordinates.latitude.toFixed(5));
    setLongitudeInput(coordinates.longitude.toFixed(5));
  }, []);

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

  const updateCoordinateInputs = (
    latitudeText: string,
    longitudeText: string,
  ) => {
    setLatitudeInput(latitudeText);
    setLongitudeInput(longitudeText);

    if (latitudeText.trim() === "" || longitudeText.trim() === "") {
      setSelection(null);
      return;
    }

    const latitude = Number(latitudeText);
    const longitude = Number(longitudeText);
    if (isValidCoordinates(latitude, longitude)) {
      setSelection({ latitude, longitude });
    } else {
      setSelection(null);
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
