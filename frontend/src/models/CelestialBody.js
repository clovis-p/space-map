import { SpaceObject } from './SpaceObject.js';
import { keplerianToPosition } from '../services/propagator.js';

/**
 * A natural celestial body (planet, moon, asteroid, comet, dwarf planet, star).
 * Position is propagated from Keplerian orbital elements.
 */
export class CelestialBody extends SpaceObject {
  /**
   * @param {object} params
   * @param {string} params.id
   * @param {string} params.name
   * @param {number} params.radiusKm Physical radius in kilometers
   * @param {number} params.color Hex color for rendering (e.g. 0x4fc3f7)
   * @param {object} params.elements Keplerian orbital elements
   * @param {number} params.elements.semiMajorAxis In AU
   * @param {number} params.elements.eccentricity
   * @param {number} params.elements.inclination In degrees
   * @param {number} params.elements.longitudeOfAscendingNode In degrees
   * @param {number} params.elements.argumentOfPeriapsis In degrees
   * @param {number} params.elements.meanAnomalyAtEpoch In degrees, at J2000 epoch
   * @param {number} params.elements.meanMotion In degrees/day
   */
  constructor({ id, name, radiusKm, color, elements }) {
    super({ id, name, type: 'celestial', displayMode: 'mesh' });
    this.radiusKm = radiusKm;
    this.color = color;
    this.elements = elements;
  }

  /**
   * @param {number} t Unix timestamp in milliseconds
   * @returns {{ x: number, y: number, z: number }} Position in AU
   */
  getPosition(t) {
    if (!this.elements) return { x: 0, y: 0, z: 0 };
    return keplerianToPosition(this.elements, t);
  }
}
