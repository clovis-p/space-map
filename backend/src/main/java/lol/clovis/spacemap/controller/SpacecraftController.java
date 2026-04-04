package lol.clovis.spacemap.controller;

import lol.clovis.spacemap.model.GroupInfo;
import lol.clovis.spacemap.model.SpacecraftData;
import lol.clovis.spacemap.service.SpacecraftSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class SpacecraftController {

    private final Map<String, SpacecraftSource> groupRegistry;

    public SpacecraftController(List<SpacecraftSource> sources) {
        this.groupRegistry = sources.stream()
                .flatMap(s -> s.groups().keySet().stream().map(id -> Map.entry(id, s)))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @GetMapping("/spacecraft")
    public List<SpacecraftData> getSpacecraft(@RequestParam(defaultValue = "stations") String group) {
        SpacecraftSource source = groupRegistry.get(group);
        if (source == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown group '%s'. See /api/groups for valid options.".formatted(group));
        }
        return source.fetchGroup(group);
    }

    @GetMapping("/groups")
    public List<GroupDto> getGroups() {
        return groupRegistry.entrySet().stream()
                .map(e -> {
                    GroupInfo info = e.getValue().groups().get(e.getKey());
                    return new GroupDto(e.getKey(), info.name(), info.color());
                })
                .sorted(Comparator.comparing(GroupDto::name))
                .toList();
    }

    public record GroupDto(String id, String name, int color) {}
}
