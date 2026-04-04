package lol.clovis.spacemap.model;

public record OrbitalElements(
        double semiMajorAxis,
        double eccentricity,
        double inclination,
        double longitudeOfAscendingNode,
        double argumentOfPeriapsis,
        double meanAnomalyAtEpoch,
        double meanMotion,
        String epoch
) {}
