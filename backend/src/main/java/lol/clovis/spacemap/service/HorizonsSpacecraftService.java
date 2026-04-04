package lol.clovis.spacemap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lol.clovis.spacemap.model.GroupInfo;
import lol.clovis.spacemap.model.OrbitalElements;
import lol.clovis.spacemap.model.SpacecraftData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
public class HorizonsSpacecraftService implements SpacecraftSource {

    private static final Logger log = LoggerFactory.getLogger(HorizonsSpacecraftService.class);

    private record SpacecraftInfo(
            String name,
            String horizonsId,
            String center,
            String centralBodyId) {}

    private static final Map<String, SpacecraftInfo> SPACECRAFT = Map.of(
            "juno", new SpacecraftInfo("Juno", "-61", "500@599", "jupiter"),
            "jwst", new SpacecraftInfo("JWST", "-170", "500@10", "sun"),
            "new-horizons", new SpacecraftInfo("New Horizons", "-98", "500@10", "sun"),
            "voyager-1", new SpacecraftInfo("Voyager 1", "-31", "500@10", "sun"),
            "voyager-2", new SpacecraftInfo("Voyager 2", "-32", "500@10", "sun")
    );

    private static final Map<String, GroupInfo> OWN_GROUPS =
            Map.of("interplanetary", new GroupInfo("Interplanetary", 0xf57c00));

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String horizonsUrl;

    public HorizonsSpacecraftService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper,
                                     @Value("${horizons.api.url}") String horizonsUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = restClientBuilder.requestFactory(factory).build();
        this.objectMapper = objectMapper;
        this.horizonsUrl = horizonsUrl;
    }

    @Override
    public Map<String, GroupInfo> groups() {
        return OWN_GROUPS;
    }

    @Override
    @Cacheable("spacecraft")
    public List<SpacecraftData> fetchGroup(String groupId) {
        if (!OWN_GROUPS.containsKey(groupId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown group '%s'. See /api/groups for valid options.".formatted(groupId));
        }
        log.info("Fetching orbital elements for interplanetary spacecraft from JPL Horizons");
        return SPACECRAFT.entrySet().stream()
                .map(e -> fetchOne(e.getKey(), e.getValue()))
                .toList();
    }

    private SpacecraftData fetchOne(String id, SpacecraftInfo info) {
        OrbitalElements elements = fetchElements(info.horizonsId(), info.center());
        return new SpacecraftData(id, info.name(), info.centralBodyId(), elements);
    }

    private OrbitalElements fetchElements(String horizonsId, String center) {
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        String tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1).toString();
        String encodedCenter = center.replace("@", "%40");
        String url = horizonsUrl
                + "?format=json&COMMAND=" + horizonsId
                + "&CENTER=" + encodedCenter
                + "&EPHEM_TYPE=ELEMENTS"
                + "&OUT_UNITS=AU-D"
                + "&OBJ_DATA=NO"
                + "&MAKE_EPHEM=YES"
                + "&START_TIME=" + today
                + "&STOP_TIME=" + tomorrow
                + "&STEP_SIZE=1d";
        String raw = restClient.get().uri(URI.create(url)).retrieve().body(String.class);
        try {
            String result = objectMapper.readTree(raw).get("result").asText();
            return HorizonsPlanetService.parseElements(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Horizons response for " + horizonsId + ": " + e.getMessage(), e);
        }
    }
}
