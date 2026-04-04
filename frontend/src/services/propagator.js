/**
 * Keplerian orbital mechanics propagator.
 * Converts orbital elements to a heliocentric ecliptic J2000 position at time t.
 *
 * Reference: https://ssd.jpl.nasa.gov/planets/approx_pos.html
 */

const DEG = Math.PI / 180;

// J2000 epoch as Unix timestamp (ms)
const J2000_MS = 946728000000;

/**
 * Solve Kepler's equation M = E - e*sin(E) for E using Newton-Raphson iteration.
 * @param {number} M Mean anomaly in radians
 * @param {number} e Eccentricity
 * @returns {number} Eccentric anomaly in radians
 */
function solveKepler(M, e) {
  let E = M;
  for (let i = 0; i < 50; i++) {
    const dE = (M - (E - e * Math.sin(E))) / (1 - e * Math.cos(E));
    E += dE;
    if (Math.abs(dE) < 1e-10) {break;}
  }
  return E;
}

/**
 * Solve hyperbolic Kepler's equation M = e*sinh(H) - H for H using Newton-Raphson iteration.
 * @param {number} M Hyperbolic mean anomaly in radians
 * @param {number} e Eccentricity (must be > 1)
 * @returns {number} Hyperbolic anomaly H
 */
function solveKeplerHyperbolic(M, e) {
  let H = Math.sign(M) * Math.log(2 * Math.abs(M) / e + 1.8);
  for (let i = 0; i < 50; i++) {
    const dH = (M - e * Math.sinh(H) + H) / (e * Math.cosh(H) - 1);
    H += dH;
    if (Math.abs(dH) < 1e-10) {break;}
  }
  return H;
}

/**
 * Convert Keplerian elements to a heliocentric ecliptic J2000 cartesian position.
 *
 * @param {object} elements
 * @param {number} elements.semiMajorAxis a, in AU
 * @param {number} elements.eccentricity e
 * @param {number} elements.inclination i, in degrees
 * @param {number} elements.longitudeOfAscendingNode Ω, in degrees
 * @param {number} elements.argumentOfPeriapsis ω, in degrees
 * @param {number} elements.meanAnomalyAtEpoch M0, in degrees at J2000
 * @param {number} elements.meanMotion n, in degrees/day
 * @param {number} t Unix timestamp in milliseconds
 * @returns {{ x: number, y: number, z: number }} Position in AU
 */
export function keplerianToPosition(elements, t) {
  const { semiMajorAxis: a, eccentricity: e, inclination, longitudeOfAscendingNode, argumentOfPeriapsis, meanAnomalyAtEpoch, meanMotion, epoch } = elements;

  const epochMs = epoch ? new Date(epoch).getTime() : J2000_MS;
  const daysSinceEpoch = (t - epochMs) / 86400000;

  const rawM = meanAnomalyAtEpoch + meanMotion * daysSinceEpoch;
  // Hyperbolic orbits: M grows unbounded, no wrapping. Elliptical: wrap to [0, 360).
  const M = (e >= 1 ? rawM : rawM % 360) * DEG;

  let r, nu;
  if (e >= 1) {
    // Hyperbolic orbit: solve M = e*sinh(H) - H
    const H = solveKeplerHyperbolic(M, e);
    nu = 2 * Math.atan2(Math.sqrt(e + 1) * Math.sinh(H / 2), Math.sqrt(e - 1) * Math.cosh(H / 2));
    r = a * (1 - e * Math.cosh(H)); // a < 0 for hyperbolic, so r > 0
  } else {
    // Elliptical orbit: solve M = E - e*sin(E)
    const E = solveKepler(M, e);
    nu = 2 * Math.atan2(Math.sqrt(1 + e) * Math.sin(E / 2), Math.sqrt(1 - e) * Math.cos(E / 2));
    r = a * (1 - e * Math.cos(E));
  }

  // Position in orbital plane
  const xOrbital = r * Math.cos(nu);
  const yOrbital = r * Math.sin(nu);

  // Convert to 3D heliocentric ecliptic using Euler rotations
  const i = inclination * DEG;
  const Omega = longitudeOfAscendingNode * DEG;
  const omega = argumentOfPeriapsis * DEG;

  const cosOmega = Math.cos(Omega), sinOmega = Math.sin(Omega);
  const cosOmega2 = Math.cos(omega), sinOmega2 = Math.sin(omega);
  const cosI = Math.cos(i), sinI = Math.sin(i);

  const x = (cosOmega * cosOmega2 - sinOmega * sinOmega2 * cosI) * xOrbital
          + (-cosOmega * sinOmega2 - sinOmega * cosOmega2 * cosI) * yOrbital;

  const y = (sinOmega * cosOmega2 + cosOmega * sinOmega2 * cosI) * xOrbital
          + (-sinOmega * sinOmega2 + cosOmega * cosOmega2 * cosI) * yOrbital;

  const z = (sinOmega2 * sinI) * xOrbital
          + (cosOmega2 * sinI) * yOrbital;

  return { x, y, z };
}
