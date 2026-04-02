import * as THREE from 'three';

// True scale: 1 AU = 149,597,871 km. Positions are in AU, so radii must use the same.
// At true scale planets are invisible dots : MIN_BODY_RADIUS gives them a floor.
const KM_PER_AU = 149597871;
const RADIUS_SCALE = 1 / KM_PER_AU;  // km → AU (scene units)
const MIN_BODY_RADIUS = 0.002;        // scene units : keeps small bodies visible

/**
 * Creates and updates Three.js meshes and sprites for SpaceObjects.
 * Positions are always set from getPosition(t) : never hardcoded.
 */
export class ObjectRenderer {
  constructor(scene) {
    this.scene = scene;
    // Map from body.id → THREE.Object3D
    this._objects = new Map();
  }

  /**
   * Add a CelestialBody to the scene.
   * @param {import('../../../models/CelestialBody.js').CelestialBody} body
   */
  addCelestialBody(body) {
    const radius = Math.max(body.radiusKm * RADIUS_SCALE, MIN_BODY_RADIUS);
    const geometry = new THREE.SphereGeometry(radius, 32, 16);
    const material = new THREE.MeshStandardMaterial({ color: body.color, roughness: 0.8 });
    const mesh = new THREE.Mesh(geometry, material);
    mesh.userData.bodyId = body.id;

    // Sun: emissive so it glows regardless of lighting
    if (body.id === 'sun') {
      material.emissive = new THREE.Color(body.color);
      material.emissiveIntensity = 1;
    }

    this.scene.add(mesh);
    this._objects.set(body.id, mesh);
    return mesh;
  }

  /**
   * Update the 3D position of all tracked objects.
   * @param {Map<string, import('../../../models/SpaceObject.js').SpaceObject>} bodies
   * @param {number} t Unix timestamp in ms
   */
  updatePositions(bodies, t) {
    for (const [id, obj] of this._objects) {
      const body = bodies.get(id);
      if (!body) {continue;}

      if (body.id === 'sun') {
        obj.position.set(0, 0, 0);
        continue;
      }

      const pos = body.getPosition(t);
      // Three.js uses y-up; ecliptic plane is XZ, inclination lifts into Y
      obj.position.set(pos.x, pos.z, pos.y);
    }
  }

  dispose() {
    for (const obj of this._objects.values()) {
      obj.geometry.dispose();
      obj.material.dispose();
      this.scene.remove(obj);
    }
    this._objects.clear();
  }
}
