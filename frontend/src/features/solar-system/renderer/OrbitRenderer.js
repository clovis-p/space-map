import * as THREE from 'three';
import { keplerianToPosition } from '../../../services/propagator.js';

const ORBIT_SEGMENTS = 256;
// A fixed reference time for sampling planet orbit points (J2000 epoch, ms)
const J2000_MS = 946728000000;

/**
 * Renders orbital ellipses as THREE.Line objects.
 * Planet orbits are computed once at load time.
 * The active satellite orbit is recomputed on focus and replaced on each selection.
 */
export class OrbitRenderer {
  constructor(scene) {
    this.scene = scene;
    this._lines = [];
    this._spacecraftOrbitLine = null;
  }

  /**
   * Draw an orbit ellipse for a CelestialBody.
   * @param {import('../../../models/CelestialBody.js').CelestialBody} body
   */
  addOrbit(body) {
    if (!body.elements) {return;} // Sun has no orbit
    const line = this._buildOrbitLine(body.elements, { color: body.color });
    line.userData.bodyId = body.id;
    this.scene.add(line);
    this._lines.push(line);
  }

  /**
   * Draw the orbit ellipse for a spacecraft, offset by its central body's current position.
   * Only one spacecraft orbit is shown at a time; calling this replaces any previous one.
   * @param {import('../../../models/Spacecraft.js').Spacecraft} spacecraft
   * @param {{ x: number, y: number, z: number }} centralBodyEclipticPos Heliocentric ecliptic position in AU
   */
  addSpacecraftOrbit(spacecraft, centralBodyEclipticPos) {
    this.clearSpacecraftOrbit();
    if (!spacecraft.elements) return;
    const line = this._buildOrbitLine(spacecraft.elements, {
      tRef: Date.now(),
      color: 0xffffff,
      opacity: 0.6,
      offset: centralBodyEclipticPos,
    });
    line.frustumCulled = false;
    this.scene.add(line);
    this._spacecraftOrbitLine = line;
  }

  /**
   * Remove the active spacecraft orbit line from the scene.
   */
  clearSpacecraftOrbit() {
    if (!this._spacecraftOrbitLine) return;
    this._spacecraftOrbitLine.geometry.dispose();
    this._spacecraftOrbitLine.material.dispose();
    this.scene.remove(this._spacecraftOrbitLine);
    this._spacecraftOrbitLine = null;
  }

  /**
   * Build a THREE.Line tracing one full orbital period.
   * @param {object} elements Keplerian orbital elements
   * @param {object} [opts]
   * @param {number} [opts.tRef] Reference epoch in ms (default: J2000)
   * @param {number} [opts.color] Line color (default: white)
   * @param {number} [opts.opacity] Line opacity (default: 0.3)
   * @param {{ x: number, y: number, z: number } | null} [opts.offset] Ecliptic position offset in AU (default: none)
   */
  _buildOrbitLine(elements, { tRef = J2000_MS, color = 0xffffff, opacity = 0.3, offset = null } = {}) {
    const period = 360 / elements.meanMotion; // days
    const ox = offset?.x ?? 0;
    const oy = offset?.y ?? 0;
    const oz = offset?.z ?? 0;
    const points = [];
    for (let i = 0; i <= ORBIT_SEGMENTS; i++) {
      const t = tRef + (i / ORBIT_SEGMENTS) * period * 86400000;
      const pos = keplerianToPosition(elements, t);
      // y-up: ecliptic (x,y,z) maps to three.js (x,z,y)
      points.push(new THREE.Vector3(ox + pos.x, oz + pos.z, oy + pos.y));
    }
    const geometry = new THREE.BufferGeometry().setFromPoints(points);
    const material = new THREE.LineBasicMaterial({ color, opacity, transparent: true });
    return new THREE.Line(geometry, material);
  }

  dispose() {
    for (const line of this._lines) {
      line.geometry.dispose();
      line.material.dispose();
      this.scene.remove(line);
    }
    this._lines = [];
    this.clearSpacecraftOrbit();
  }
}
