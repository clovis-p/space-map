package lol.clovis.spacemap.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Provides solar system body data (Sun + 8 planets).
 *
 * Keplerian elements are mean orbital elements at the J2000 epoch, sourced from:
 * <a href="https://ssd.jpl.nasa.gov/planets/approx_pos.html">...</a>
 *
 * The frontend propagates positions client-side using these elements, so they must
 * be J2000-anchored — do not replace with live osculating elements from Horizons.
 */
@Service
public class PlanetService {

    private static final List<Body> BODIES = List.of(
            new Body("sun", "Sun", 696000, 0xffd700, null),
            new Body("mercury", "Mercury", 2439.7, 0xb5b5b5, new OrbitalElements(
                    0.38709927, 0.20563593, 7.00497902,
                    48.33076593, 29.12703035, 174.79252722, 4.09233445)),
            new Body("venus", "Venus", 6051.8, 0xe8cda0, new OrbitalElements(
                    0.72333566, 0.00677672, 3.39467605,
                    76.67984255, 54.92262463, 50.37663232, 1.60213034)),
            new Body("earth", "Earth", 6371.0, 0x4fc3f7, new OrbitalElements(
                    1.00000261, 0.01671123, -0.00001531,
                    0.0, 102.93768193, 100.46457166, 0.98560028)),
            new Body("mars", "Mars", 3389.5, 0xc1440e, new OrbitalElements(
                    1.52371034, 0.09339410, 1.84969142,
                    49.55953891, 286.49748, 355.44306, 0.52402068)),
            new Body("jupiter", "Jupiter", 69911.0, 0xc88b3a, new OrbitalElements(
                    5.20288700, 0.04838624, 1.30439695,
                    100.47390909, 273.86712, 34.39644051, 0.08308676)),
            new Body("saturn", "Saturn", 58232.0, 0xe4d191, new OrbitalElements(
                    9.53667594, 0.05386179, 2.48599187,
                    113.66242448, 339.39149, 49.95424423, 0.03344414)),
            new Body("uranus", "Uranus", 25362.0, 0x7de8e8, new OrbitalElements(
                    19.18916464, 0.04725744, 0.77263783,
                    74.01692503, 96.99895829, 313.23810451, 0.01172834)),
            new Body("neptune", "Neptune", 24622.0, 0x3f54ba, new OrbitalElements(
                    30.06992276, 0.00859048, 1.77004347,
                    131.78422574, 276.34, 304.88003, 0.00598103))
    );

    public List<Body> getAllBodies() {
        return BODIES;
    }

    public record OrbitalElements(
            double semiMajorAxis,
            double eccentricity,
            double inclination,
            double longitudeOfAscendingNode,
            double argumentOfPeriapsis,
            double meanAnomalyAtEpoch,
            double meanMotion
    ) {}

    public record Body(
            String id,
            String name,
            double radiusKm,
            int color,
            OrbitalElements elements
    ) {}
}
