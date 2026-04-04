package lol.clovis.spacemap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lol.clovis.spacemap.service.CelestrakService;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SatelliteController {

    private final CelestrakService celestrakService;

    public SatelliteController(CelestrakService celestrakService) {
        this.celestrakService = celestrakService;
    }

    /**
     * Returns raw OMM JSON from CelesTrak for the requested group.
     * Results are cached for 30 minutes.
     *
     * Example: GET /api/satellites?group=stations
     */
    @GetMapping("/satellites")
    public List<CelestrakService.SatelliteData> getSatellites(@RequestParam(defaultValue = "stations") String group) {
        return celestrakService.fetchGroup(group);
    }

    /**
     * Returns the list of supported satellite groups with their display names.
     *
     * Example: GET /api/groups
     */
    @GetMapping("/groups")
    public List<GroupDto> getGroups() {
        return CelestrakService.GROUPS.entrySet().stream()
                .map(e -> new GroupDto(e.getKey(), e.getValue().name(), e.getValue().color()))
                .sorted(Comparator.comparing(GroupDto::name))
                .toList();
    }

    public record GroupDto(String id, String name, int color) {}
}
