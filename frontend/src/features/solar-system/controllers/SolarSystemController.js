import { SceneRenderer } from '../renderer/SceneRenderer.js';
import { OrbitRenderer } from '../renderer/OrbitRenderer.js';
import { ObjectRenderer } from '../renderer/ObjectRenderer.js';
import { CelestialBody } from '../../../models/CelestialBody.js';
import { Satellite } from '../../../models/Satellite.js';
import { fetchBodies } from '../../../services/api.js';

/**
 * Orchestrates the solar system scene.
 * Owns the animation loop. Never holds Three.js objects in Vue reactive state.
 *
 * Time is always passed as an explicit parameter `t` so that time scrubbing
 * can be wired in later by simply changing what value is passed.
 */
export class SolarSystemController {
  /**
   * @param {HTMLCanvasElement} canvas
   * @param {object} [callbacks]
   * @param {(bodies: Map) => void} [callbacks.onBodiesLoaded] Called once bodies are ready
   * @param {() => void} [callbacks.onAfterRender] Called every frame after render, use for label updates
   */
  constructor(canvas, callbacks = {}) {
    this._callbacks = callbacks;
    this._sceneRenderer = new SceneRenderer(canvas);
    this._orbitRenderer = new OrbitRenderer(this._sceneRenderer.scene);
    this._objectRenderer = new ObjectRenderer(this._sceneRenderer.scene);
    /** @type {Map<string, import('../../../models/SpaceObject.js').SpaceObject>} */
    this._bodies = new Map();
    /** @type {Map<string, Satellite>} */
    this._satellites = new Map();
    this._animFrameId = null;
    // Time override: null means use real-time (Date.now())
    this._timeOverride = null;
    this._focusedBodyId = null;
    this._focusedSatelliteId = null;

    this._load();
  }

  async _load() {
    const rawBodies = await fetchBodies();

    for (const data of rawBodies) {
      const body = new CelestialBody(data);
      this._bodies.set(body.id, body);
      this._objectRenderer.addCelestialBody(body);
      this._orbitRenderer.addOrbit(body);
    }

    this._callbacks.onBodiesLoaded?.(this._bodies);
    this._startLoop();
  }

  _startLoop() {
    const tick = () => {
      this._animFrameId = requestAnimationFrame(tick);
      const t = this._timeOverride ?? Date.now();
      this._objectRenderer.updatePositions(this._bodies, t);
      this._objectRenderer.updateSatellitePoints(this._bodies, this._satellites, t);
      this._trackFocus(t);
      this._sceneRenderer.render();
      this._callbacks.onAfterRender?.();
    };
    tick();
  }

  _trackFocus(t) {
    if (this._focusedSatelliteId) {
      const sat = this._satellites.get(this._focusedSatelliteId);
      if (!sat) return;
      const earth = this._bodies.get(sat.centralBodyId);
      if (!earth) return;
      const satRel = sat.getPosition(t);
      const earthPos = earth.getPosition(t);
      // heliocentric ecliptic → three.js y-up: (x,y,z) → (x,z,y)
      this._sceneRenderer.setFocusTarget(
        earthPos.x + satRel.x,
        earthPos.z + satRel.z,
        earthPos.y + satRel.y,
      );
      return;
    }
    if (!this._focusedBodyId) return;
    const body = this._bodies.get(this._focusedBodyId);
    if (!body) return;
    if (body.id === 'sun') {
      this._sceneRenderer.setFocusTarget(0, 0, 0);
      return;
    }
    const pos = body.getPosition(t);
    // y-up: ecliptic y → Three.js z, ecliptic z → Three.js y
    this._sceneRenderer.setFocusTarget(pos.x, pos.z, pos.y);
  }

  /**
   * Smoothly move the camera to orbit around a body.
   * @param {string} id
   */
  focusBody(id) {
    this._focusedSatelliteId = null;
    this._focusedBodyId = id;
  }

  /**
   * Load a satellite group into the scene as a point cloud.
   * @param {Array} rawArray Raw satellite data from the API
   */
  loadSatellites(rawArray) {
    this._objectRenderer.clearSatellitePoints();
    this._orbitRenderer.clearSatelliteOrbit();
    this._satellites.clear();
    this._focusedSatelliteId = null;
    for (const data of rawArray) {
      const sat = new Satellite(data);
      this._satellites.set(sat.id, sat);
    }
    this._objectRenderer.setSatellitePoints(this._satellites);
  }

  /**
   * Remove all satellite visuals from the scene.
   */
  clearSatellites() {
    this._objectRenderer.clearSatellitePoints();
    this._orbitRenderer.clearSatelliteOrbit();
    this._satellites.clear();
    this._focusedSatelliteId = null;
  }

  /**
   * Focus the camera on a satellite and draw its orbit.
   * @param {string} id NORAD catalog number
   */
  focusSatellite(id) {
    const sat = this._satellites.get(id);
    if (!sat) return;
    const earth = this._bodies.get(sat.centralBodyId);
    if (!earth) return;

    this._orbitRenderer.clearSatelliteOrbit();

    const t = this._timeOverride ?? Date.now();
    const earthPos = earth.getPosition(t);
    this._orbitRenderer.addSatelliteOrbit(sat, earthPos);

    this._focusedBodyId = null;
    this._focusedSatelliteId = id;
    this._sceneRenderer.clearFocusTarget();
    this._zoomToSatellite(earthPos, sat.elements.semiMajorAxis * 3);
  }

  _zoomToSatellite(earthEclipticPos, dist) {
    // Convert Earth ecliptic position to three.js y-up coords
    const ex = earthEclipticPos.x;
    const ey = earthEclipticPos.z;
    const ez = earthEclipticPos.y;

    const cam = this._sceneRenderer.camera;
    const target = this._sceneRenderer.controls.target;
    const dir = cam.position.clone().sub(target);
    if (dir.length() < 0.0001) dir.set(0, 1, 0);
    else dir.normalize();

    cam.position.set(ex + dir.x * dist, ey + dir.y * dist, ez + dir.z * dist);
    this._sceneRenderer.controls.target.set(ex, ey, ez);
    this._sceneRenderer.controls.update();
  }

  /**
   * Override the current time. Pass null to return to real-time.
   * @param {number|null} t Unix timestamp in ms, or null for real-time
   */
  setTime(t) {
    this._timeOverride = t;
  }

  dispose() {
    if (this._animFrameId !== null) cancelAnimationFrame(this._animFrameId);
    this._objectRenderer.dispose();
    this._orbitRenderer.dispose();
    this._sceneRenderer.dispose();
  }
}
