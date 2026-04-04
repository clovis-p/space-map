import * as THREE from 'three';

// 1 AU = 149,597,871 km. All positions and radii are in AU.
const KM_PER_AU = 149597871;
const RADIUS_SCALE = 1 / KM_PER_AU;  // km to AU (scene units)

/**
 * Creates and updates Three.js meshes and sprites for SpaceObjects.
 * Positions are always set from getPosition(t) : never hardcoded.
 */
export class ObjectRenderer {
  constructor(scene) {
    this.scene = scene;
    // Map from body.id to THREE.Object3D
    this._objects = new Map();
    this._spacecraftPoints = null;
    this._spacecraftIds = [];
  }

  /**
   * Add a CelestialBody to the scene.
   * @param {import('../../../models/CelestialBody.js').CelestialBody} body
   */
  addCelestialBody(body) {
    const radius = body.radiusKm * RADIUS_SCALE;
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

  /**
   * Create a point cloud for all spacecraft in the given map.
   * Points use a fixed pixel size so they stay visible at any zoom level.
   * @param {Map<string, import('../../../models/Spacecraft.js').Spacecraft>} spacecraftMap
   */
  setSpacecraftPoints(spacecraftMap) {
    this.clearSpacecraftPoints();
    const count = spacecraftMap.size;
    if (count === 0) return;

    this._spacecraftIds = [...spacecraftMap.keys()];
    const positions = new Float32Array(count * 3);
    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    const material = new THREE.PointsMaterial({
      color: 0xffffff,
      size: 2,
      sizeAttenuation: false,
    });
    this._spacecraftPoints = new THREE.Points(geometry, material);
    this._spacecraftPoints.frustumCulled = false;
    this.scene.add(this._spacecraftPoints);
  }

  /**
   * Update spacecraft point positions every frame.
   * @param {Map<string, import('../../../models/SpaceObject.js').SpaceObject>} bodies Planet map, used to look up central bodies
   * @param {Map<string, import('../../../models/Spacecraft.js').Spacecraft>} spacecraft
   * @param {number} t Unix timestamp in ms
   */
  updateSpacecraftPoints(bodies, spacecraft, t) {
    if (!this._spacecraftPoints) return;
    const positions = this._spacecraftPoints.geometry.attributes.position.array;
    for (let i = 0; i < this._spacecraftIds.length; i++) {
      const sat = spacecraft.get(this._spacecraftIds[i]);
      const earth = sat ? bodies.get(sat.centralBodyId) : null;
      if (!sat || !earth) {
        positions[i * 3] = positions[i * 3 + 1] = positions[i * 3 + 2] = 0;
        continue;
      }
      const satRel = sat.getPosition(t);
      const earthP = earth.getPosition(t);
      // Heliocentric ecliptic coords, then y-up: ecliptic (x,y,z) maps to three.js (x,z,y)
      positions[i * 3] = earthP.x + satRel.x;
      positions[i * 3 + 1] = earthP.z + satRel.z;
      positions[i * 3 + 2] = earthP.y + satRel.y;
    }
    this._spacecraftPoints.geometry.attributes.position.needsUpdate = true;
  }

  /**
   * Remove the spacecraft point cloud from the scene and free its resources.
   */
  clearSpacecraftPoints() {
    if (!this._spacecraftPoints) return;
    this._spacecraftPoints.geometry.dispose();
    this._spacecraftPoints.material.dispose();
    this.scene.remove(this._spacecraftPoints);
    this._spacecraftPoints = null;
    this._spacecraftIds = [];
  }

  dispose() {
    for (const obj of this._objects.values()) {
      obj.geometry.dispose();
      obj.material.dispose();
      this.scene.remove(obj);
    }
    this._objects.clear();
    this.clearSpacecraftPoints();
  }
}
