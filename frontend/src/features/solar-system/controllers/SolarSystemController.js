import { SceneRenderer } from '../renderer/SceneRenderer.js';
import { OrbitRenderer } from '../renderer/OrbitRenderer.js';
import { ObjectRenderer } from '../renderer/ObjectRenderer.js';
import { CelestialBody } from '../../../models/CelestialBody.js';
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
    this._animFrameId = null;
    // Time override: null means use real-time (Date.now())
    this._timeOverride = null;
    this._focusedBodyId = null;

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
      this._trackFocus(t);
      this._sceneRenderer.render();
      this._callbacks.onAfterRender?.();
    };
    tick();
  }

  _trackFocus(t) {
    if (!this._focusedBodyId) {
      return;
    }
    const body = this._bodies.get(this._focusedBodyId);
    if (!body) {
      return;
    }
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
    this._focusedBodyId = id;
  }

  /**
   * Override the current time. Pass null to return to real-time.
   * @param {number|null} t Unix timestamp in ms, or null for real-time
   */
  setTime(t) {
    this._timeOverride = t
  }

  dispose() {
    if (this._animFrameId !== null) cancelAnimationFrame(this._animFrameId)
    this._objectRenderer.dispose()
    this._orbitRenderer.dispose()
    this._sceneRenderer.dispose()
  }
}
