import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';

/**
 * Manages the Three.js scene, camera, renderer, and controls.
 * Does not know about SpaceObjects : just raw Three.js primitives.
 */
export class SceneRenderer {
  constructor(canvas) {
    this.canvas = canvas;
    this._initRenderer();
    this._initScene();
    this._initCamera();
    this._initLights();
    this._initControls();
    this._initResizeObserver();
  }

  _initRenderer() {
    this.renderer = new THREE.WebGLRenderer({ canvas: this.canvas, antialias: true, logarithmicDepthBuffer: true });
    this.renderer.setPixelRatio(window.devicePixelRatio);
    this.renderer.setSize(this.canvas.clientWidth, this.canvas.clientHeight);
  }

  _initScene() {
    this.scene = new THREE.Scene();
  }

  _initCamera() {
    const w = this.canvas.clientWidth;
    const h = this.canvas.clientHeight;
    this.camera = new THREE.PerspectiveCamera(60, w / h, 0.0000001, 10000);
    // Position camera above the ecliptic plane looking down
    this.camera.position.set(0, 30, 0);
    this.camera.lookAt(0, 0, 0);
  }

  _initLights() {
    // Ambient light so dark sides of planets are barely visible
    this.scene.add(new THREE.AmbientLight(0xffffff, 0.05));
    // Sun as a point light at the origin
    const sunLight = new THREE.PointLight(0xffffff, 2, 0, 1);
    sunLight.position.set(0, 0, 0);
    this.scene.add(sunLight);
  }

  _initControls() {
    this.controls = new OrbitControls(this.camera, this.canvas);
    this.controls.enableDamping = true;
    this.controls.dampingFactor = 0.05;
    this.controls.minDistance = 0.00001;
    this.controls.maxDistance = 500;
    // Focus tracking state
    this._focusTarget = null;     // current world position of focused body
    this._focusTransitioning = false;
  }

  /**
   * Called every frame with the focused body's current world position.
   *
   * Two phases:
   *  - Transitioning: lerp controls.target toward the body (smooth pan on focus click)
   *  - Tracking: apply the body's movement delta to both controls.target and camera.position
   *    so OrbitControls' internal spherical coords are never touched — no zoom drift, no lag.
   *
   * @param {number} x
   * @param {number} y
   * @param {number} z
   */
  setFocusTarget(x, y, z) {
    if (!this._focusTarget) {
      this._focusTarget = new THREE.Vector3(x, y, z);
      this._focusTransitioning = true;
      return;
    }

    const newPos = new THREE.Vector3(x, y, z);

    if (!this._focusTransitioning) {
      // Tracking phase: translate camera frame of reference by the body's movement
      const delta = newPos.clone().sub(this._focusTarget);
      this.controls.target.add(delta);
      this.camera.position.add(delta);
    }

    this._focusTarget.copy(newPos);
  }

  clearFocusTarget() {
    this._focusTarget = null;
    this._focusTransitioning = false;
  }

  _initResizeObserver() {
    this._resizeObserver = new ResizeObserver(() => this._onResize());
    this._resizeObserver.observe(this.canvas.parentElement ?? this.canvas);
  }

  _onResize() {
    const w = this.canvas.clientWidth;
    const h = this.canvas.clientHeight;
    this.camera.aspect = w / h;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(w, h);
  }

  render() {
    if (this._focusTarget && this._focusTransitioning) {
      this.controls.target.lerp(this._focusTarget, 0.1);
      if (this.controls.target.distanceTo(this._focusTarget) < 0.001) {
        this.controls.target.copy(this._focusTarget);
        this._focusTransitioning = false;
      }
    }
    this.controls.update();
    this.renderer.render(this.scene, this.camera);
  }

  dispose() {
    this._resizeObserver.disconnect();
    this.controls.dispose();
    this.renderer.dispose();
  }
}
