package lol.clovis.spacemap.model;

public record SpacecraftData(
        String id,
        String name,
        String centralBodyId,
        OrbitalElements elements
) {}
