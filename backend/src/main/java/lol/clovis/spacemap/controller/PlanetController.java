package lol.clovis.spacemap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lol.clovis.spacemap.service.PlanetService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PlanetController {

    private final PlanetService planetService;

    public PlanetController(PlanetService planetService) {
        this.planetService = planetService;
    }

    @GetMapping("/planets")
    public List<PlanetService.Body> getPlanets() {
        return planetService.getAllBodies();
    }
}
