package lol.clovis.spacemap.controller;

import lol.clovis.spacemap.service.HorizonsPlanetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PlanetController {

    private final HorizonsPlanetService horizonsService;

    public PlanetController(HorizonsPlanetService horizonsService) {
        this.horizonsService = horizonsService;
    }

    @GetMapping("/planets")
    public List<HorizonsPlanetService.Body> getPlanets() {
        return horizonsService.getAllBodies();
    }
}
