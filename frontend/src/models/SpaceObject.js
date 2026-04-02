/**
 * Base class for all objects in the solar system.
 * Subclasses must implement getPosition(t).
 */
export class SpaceObject {
  /**
   * @param {object} params
   * @param {string} params.id
   * @param {string} params.name
   * @param {'celestial'|'satellite'} params.type
   * @param {'mesh'|'icon'} params.displayMode
   */
  constructor({ id, name, type, displayMode = 'icon' }) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.displayMode = displayMode;
  }

  /**
   * Returns the position of this object at time t.
   * @param {number} t Unix timestamp in milliseconds
   * @returns {{ x: number, y: number, z: number }} Position in AU, heliocentric ecliptic J2000
   */
  getPosition() {
    throw new Error(`${this.constructor.name} must implement getPosition(t)`);
  }
}
