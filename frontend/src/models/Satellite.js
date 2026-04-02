import { SpaceObject } from './SpaceObject.js';
import { keplerianToPosition } from '../services/propagator.js';

/**
 * A man-made satellite or spacecraft.
 * Position is propagated from Keplerian orbital elements sourced from CelesTrak OMM records.
 *
 * Note: getPosition(t) returns coordinates relative to centralBodyId (e.g. "earth"), not the Sun.
 * The rendering layer is responsible for adding the central body's heliocentric position
 * to get the satellite's position in the solar system frame.
 */
export class Satellite extends SpaceObject {
  /**
   * @param {object} params
   * @param {string} params.id NORAD catalog number
   * @param {string} params.name
   * @param {string} params.centralBodyId ID of the body this orbit is relative to (e.g. "earth")
   * @param {object} params.elements Keplerian orbital elements (semiMajorAxis in AU, epoch is TLE epoch)
   */
  constructor({ id, name, centralBodyId, elements }) {
    super({ id, name, type: 'satellite', displayMode: 'icon' });
    this.centralBodyId = centralBodyId;
    this.elements = elements;
  }

  /**
   * Position relative to centralBodyId in AU.
   * @param {number} t Unix timestamp in milliseconds
   * @returns {{ x: number, y: number, z: number }}
   */
  getPosition(t) {
    return keplerianToPosition(this.elements, t);
  }
}
