package lol.clovis.spacemap.controller;

import lol.clovis.spacemap.service.HorizonsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PlanetController {

    private final HorizonsService horizonsService;

    public PlanetController(HorizonsService horizonsService) {
        this.horizonsService = horizonsService;
    }

    @GetMapping("/planets")
    public List<HorizonsService.Body> getPlanets() {
        return horizonsService.getAllBodies();
    }
}
