import * as THREE from 'three';
import { keplerianToPosition } from '../../../services/propagator.js';

const ORBIT_SEGMENTS = 256;
// A fixed reference time for sampling orbit points (J2000 epoch, ms)
const J2000_MS = 946728000000;

/**
 * Renders orbital ellipses as THREE.Line objects.
 * Orbits are computed once and added to the scene : they don't change per frame.
 */
export class OrbitRenderer {
  constructor(scene) {
    this.scene = scene;
    this._lines = [];
  }

  /**
   * Draw an orbit ellipse for a CelestialBody.
   * @param {import('../../../models/CelestialBody.js').CelestialBody} body
   */
  addOrbit(body) {
    if (!body.elements) {return;} // Sun has no orbit

    const points = [];
    const { meanMotion } = body.elements;
    // Sample one full period
    const period = 360 / meanMotion; // days
    for (let i = 0; i <= ORBIT_SEGMENTS; i++) {
      const t = J2000_MS + (i / ORBIT_SEGMENTS) * period * 86400000;
      const pos = keplerianToPosition(body.elements, t);
      points.push(new THREE.Vector3(pos.x, pos.z, pos.y)); // y-up: swap y/z
    }

    const geometry = new THREE.BufferGeometry().setFromPoints(points);
    const material = new THREE.LineBasicMaterial({
      color: body.color,
      opacity: 0.3,
      transparent: true,
    });
    const line = new THREE.Line(geometry, material);
    line.userData.bodyId = body.id;
    this.scene.add(line);
    this._lines.push(line);
  }

  dispose() {
    for (const line of this._lines) {
      line.geometry.dispose();
      line.material.dispose();
      this.scene.remove(line);
    }
    this._lines = [];
  }
}
