export interface GeocodingResult {
  displayName: string;
  coordinates: {
    latitude: number;
    longitude: number;
  };
}

export class GeocodingApiError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "GeocodingApiError";
  }
}

const NOMINATIM_SEARCH_URL = "https://nominatim.openstreetmap.org/search";
const MIN_REQUEST_INTERVAL_MS = 1100;
const MAX_CACHED_QUERIES = 100;
const resultCache = new Map<string, GeocodingResult[]>();
let nextRequestAt = 0;

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function parseResults(payload: unknown): GeocodingResult[] {
  if (!Array.isArray(payload)) {
    throw new GeocodingApiError("El servicio de direcciones devolvió una respuesta inválida.");
  }

  return payload.map((item) => {
    if (!isRecord(item) || typeof item.display_name !== "string") {
      throw new GeocodingApiError("El servicio de direcciones devolvió un resultado inválido.");
    }

    const latitude = Number(item.lat);
    const longitude = Number(item.lon);
    if (
      !Number.isFinite(latitude) ||
      latitude < -90 ||
      latitude > 90 ||
      !Number.isFinite(longitude) ||
      longitude < -180 ||
      longitude > 180
    ) {
      throw new GeocodingApiError("El servicio de direcciones devolvió coordenadas inválidas.");
    }

    return {
      displayName: item.display_name,
      coordinates: { latitude, longitude },
    };
  });
}

function addressQueryVariants(query: string): string[] {
  const variants = [query];
  const explicitNumber = query.match(/\b(?:nro\.?|número|numero|n°)\s*(\d+[a-z]?)\b/i);

  if (explicitNumber) {
    const street = query
      .replace(explicitNumber[0], "")
      .replace(/^[\s,]+|[\s,]+$/g, "")
      .replace(/\s*,\s*/g, ", ");
    if (street) variants.push(`${explicitNumber[1]}, ${street}`);
    return variants;
  }

  const match = query.match(/^(.+?)\s+(\d+[a-z]?)(\s*,.*)?$/i);
  if (match) {
    const street = match[1].trim();
    const houseNumberFirst = `${match[2]}, ${street}${match[3] ?? ""}`;
    if (street) variants.push(houseNumberFirst);
  }

  return [...new Set(variants)];
}

async function waitForRequestSlot(signal?: AbortSignal): Promise<void> {
  const scheduledAt = Math.max(Date.now(), nextRequestAt);
  nextRequestAt = scheduledAt + MIN_REQUEST_INTERVAL_MS;
  const delay = scheduledAt - Date.now();

  if (delay > 0) {
    await new Promise<void>((resolve) => window.setTimeout(resolve, delay));
  }
  if (signal?.aborted) {
    throw new DOMException("La búsqueda fue cancelada.", "AbortError");
  }
}

export async function searchAddress(
  query: string,
  signal?: AbortSignal,
): Promise<GeocodingResult[]> {
  const normalizedQuery = query.trim().replace(/\s+/g, " ");
  if (!normalizedQuery) {
    throw new GeocodingApiError("Ingresá una dirección o lugar para buscar.");
  }

  const cacheKey = normalizedQuery.toLocaleLowerCase("es-AR");
  const cachedResults = resultCache.get(cacheKey);
  if (cachedResults) {
    if (signal?.aborted) {
      throw new DOMException("La búsqueda fue cancelada.", "AbortError");
    }
    return cachedResults;
  }

  const results: GeocodingResult[] = [];
  for (const addressQuery of addressQueryVariants(normalizedQuery)) {
    await waitForRequestSlot(signal);
    const url = new URL(NOMINATIM_SEARCH_URL);
    url.searchParams.set("q", addressQuery);
    url.searchParams.set("format", "jsonv2");
    url.searchParams.set("limit", "5");
    url.searchParams.set("accept-language", "es");
    url.searchParams.set("countrycodes", "ar");

    let response: Response;
    try {
      response = await fetch(url, {
        signal,
        headers: { Accept: "application/json" },
        referrerPolicy: "strict-origin-when-cross-origin",
      });
    } catch (error) {
      if (signal?.aborted) throw error;
      throw new GeocodingApiError(
        "No se pudo conectar con el servicio de búsqueda de direcciones. Intentá nuevamente.",
      );
    }

    if (!response.ok) {
      if (response.status === 429) {
        throw new GeocodingApiError(
          "El servicio de búsqueda está ocupado. Esperá un momento antes de volver a intentar.",
        );
      }
      throw new GeocodingApiError(
        "No se pudo completar la búsqueda de direcciones. Intentá nuevamente más tarde.",
      );
    }

    let payload: unknown;
    try {
      payload = await response.json();
    } catch {
      throw new GeocodingApiError("El servicio de búsqueda devolvió una respuesta ilegible.");
    }

    const queryResults = parseResults(payload);
    if (queryResults.length > 0) {
      results.push(...queryResults);
      break;
    }
  }

  const uniqueResults = [...new Map(
    results.map((result) => [
      `${result.coordinates.latitude},${result.coordinates.longitude}`,
      result,
    ]),
  ).values()];
  if (resultCache.size >= MAX_CACHED_QUERIES) {
    const oldestQuery = resultCache.keys().next().value;
    if (oldestQuery !== undefined) resultCache.delete(oldestQuery);
  }
  resultCache.set(cacheKey, uniqueResults);
  return uniqueResults;
}
