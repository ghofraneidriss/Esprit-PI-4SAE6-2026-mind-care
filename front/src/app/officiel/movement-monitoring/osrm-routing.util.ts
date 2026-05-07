import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

/** GeoJSON LineString or OSRM-encoded polyline string. */
export interface OsrmRouteResponse {
  code?: string;
  routes?: Array<{
    geometry?:
      | string
      | {
          type?: string;
          coordinates?: [number, number][];
        };
  }>;
}

/** Haversine distance in metres. */
export function haversineMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371000;
  const toRad = (d: number) => (d * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  return 2 * R * Math.asin(Math.min(1, Math.sqrt(a)));
}

/**
 * Google/OSRM polyline precision 5 — returns [lat, lon] pairs.
 * @see https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 */
export function decodePolyline(encoded: string, precision = 5): [number, number][] {
  const coordinates: [number, number][] = [];
  let index = 0;
  let lat = 0;
  let lng = 0;
  const factor = Math.pow(10, precision);

  while (index < encoded.length) {
    let shift = 0;
    let result = 0;
    let byte: number;
    do {
      byte = encoded.charCodeAt(index++) - 63;
      result |= (byte & 0x1f) << shift;
      shift += 5;
    } while (byte >= 0x20);
    const deltaLat = (result & 1) !== 0 ? ~(result >> 1) : result >> 1;
    lat += deltaLat;

    shift = 0;
    result = 0;
    do {
      byte = encoded.charCodeAt(index++) - 63;
      result |= (byte & 0x1f) << shift;
      shift += 5;
    } while (byte >= 0x20);
    const deltaLng = (result & 1) !== 0 ? ~(result >> 1) : result >> 1;
    lng += deltaLng;

    coordinates.push([lat / factor, lng / factor]);
  }
  return coordinates;
}

/** Extract [lon, lat][] for Leaflet from OSRM response. */
export function extractOsrmLonLatPath(res: OsrmRouteResponse): [number, number][] | null {
  let geom: unknown = res.routes?.[0]?.geometry;
  if (!geom) {
    return null;
  }
  if (typeof geom === 'object' && geom !== null && (geom as { type?: string }).type === 'Feature') {
    geom = (geom as { geometry?: { coordinates?: [number, number][] } }).geometry;
  }
  if (typeof geom === 'string') {
    const decoded = decodePolyline(geom, 5);
    if (!decoded.length) {
      return null;
    }
    return decoded.map(([lat, lon]) => [lon, lat] as [number, number]);
  }
  const coords = (geom as { coordinates?: [number, number][] }).coordinates;
  if (!Array.isArray(coords) || !coords.length) {
    return null;
  }
  return coords as [number, number][];
}

function buildOsrmUrls(lon1: number, lat1: number, lon2: number, lat2: number): string[] {
  const seg = `${lon1},${lat1};${lon2},${lat2}`;
  const qs = 'overview=full&geometries=geojson&steps=false';
  return [
    `https://router.project-osrm.org/route/v1/driving/${seg}?${qs}`,
    `https://routing.openstreetmap.de/routed-car/route/v1/driving/${seg}?${qs}`,
    `https://router.project-osrm.org/route/v1/foot/${seg}?${qs}`,
  ];
}

/**
 * Try several public OSRM mirrors (CORS / load vary). Returns lon/lat pairs or null.
 */
export async function fetchOsrmRouteLonLat(
  http: HttpClient,
  lon1: number,
  lat1: number,
  lon2: number,
  lat2: number
): Promise<[number, number][] | null> {
  for (const url of buildOsrmUrls(lon1, lat1, lon2, lat2)) {
    try {
      const res = await firstValueFrom(http.get<OsrmRouteResponse>(url));
      if (res.code === 'NoRoute') {
        continue;
      }
      const path = extractOsrmLonLatPath(res);
      if (path && path.length >= 2) {
        return path;
      }
    } catch {
      /* try next mirror */
    }
  }
  return null;
}
