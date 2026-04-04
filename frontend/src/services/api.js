/**
 * SpaceMap API client.
 * Base URL is controlled by the VITE_API_BASE_URL env var (empty string in dev, proxied by Vite).
 */

const BASE = import.meta.env.VITE_API_BASE_URL ?? '';

async function get(path) {
  const res = await fetch(`${BASE}${path}`);
  if (!res.ok) {
    throw new Error(`GET ${path} failed: ${res.status} ${res.statusText}`);
  }
  return res.json();
}

/**
 * Returns the Sun and 8 planets with visual properties and Keplerian elements.
 * @returns {Promise<Array>}
 */
export function fetchBodies() {
  return get('/api/planets');
}

/**
 * Returns available satellite groups.
 * @returns {Promise<Array>}
 */
export function fetchGroups() {
  return get('/api/groups');
}

/**
 * Returns spacecraft for a group.
 * @param {string} group Group identifier from fetchGroups()
 * @returns {Promise<Array>}
 */
export function fetchSpacecraft(group = 'stations') {
  return get(`/api/spacecraft?group=${encodeURIComponent(group)}`);
}
