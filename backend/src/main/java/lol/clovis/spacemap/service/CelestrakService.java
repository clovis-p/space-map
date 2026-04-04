package lol.clovis.spacemap.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lol.clovis.spacemap.model.GroupInfo;
import lol.clovis.spacemap.model.OrbitalElements;
import lol.clovis.spacemap.model.SpacecraftData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class CelestrakService implements SpacecraftSource {

    private static final Logger log = LoggerFactory.getLogger(CelestrakService.class);

    private static final String CELESTRAK_URL =
            "https://celestrak.org/NORAD/elements/gp.php?GROUP={group}&FORMAT=json";

    // Allowlist: protects against SSRF if the group param is ever used in a URL
    private static final Map<String, GroupInfo> OWN_GROUPS = Map.ofEntries(
            Map.entry("stations", new GroupInfo("Space Stations", 0x4fc3f7)),
            Map.entry("visual", new GroupInfo("Brightest / Visible", 0xffffff)),
            Map.entry("active", new GroupInfo("All Active", 0x80cbc4)),
            Map.entry("gps-ops", new GroupInfo("GPS", 0x66bb6a)),
            Map.entry("starlink", new GroupInfo("Starlink", 0x9575cd)),
            Map.entry("weather", new GroupInfo("Weather", 0xffca28)),
            Map.entry("noaa", new GroupInfo("NOAA", 0x42a5f5)),
            Map.entry("goes", new GroupInfo("GOES", 0x26c6da)),
            Map.entry("amateur", new GroupInfo("Amateur Radio", 0xef5350)),
            Map.entry("science", new GroupInfo("Science", 0xab47bc)),
            Map.entry("iridium-next", new GroupInfo("Iridium NEXT", 0xff7043)),
            Map.entry("oneweb", new GroupInfo("OneWeb", 0x26a69a)),
            Map.entry("galileo", new GroupInfo("Galileo", 0xffa726)),
            Map.entry("beidou", new GroupInfo("BeiDou", 0xec407a)),
            Map.entry("glonass", new GroupInfo("GLONASS", 0xff5722)),
            Map.entry("military", new GroupInfo("Military", 0x78909c)),
            Map.entry("cubesats", new GroupInfo("CubeSats", 0xd4e157))
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public CelestrakService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = restClientBuilder.requestFactory(factory).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, GroupInfo> groups() {
        return OWN_GROUPS;
    }

    private static final double MU_EARTH = 398600.4418; // km^3/s^2
    private static final double AU_IN_KM = 149597870.7;

    private static SpacecraftData toSpacecraftData(OmmRecord r) {
        double n = r.meanMotion() * 2 * Math.PI / 86400; // rad/s
        double semiMajorAxisAu = Math.pow(MU_EARTH / (n * n), 1.0 / 3.0) / AU_IN_KM;
        OrbitalElements elements = new OrbitalElements(
                semiMajorAxisAu,
                r.eccentricity(),
                r.inclination(),
                r.raOfAscNode(),
                r.argOfPericenter(),
                r.meanAnomaly(),
                r.meanMotion() * 360,
                r.epoch()
        );
        return new SpacecraftData(String.valueOf(r.noradCatId()), r.objectName(), "earth", elements);
    }

    @Override
    @Cacheable(value = "tle", key = "#group")
    public List<SpacecraftData> fetchGroup(String group) {
        if (!OWN_GROUPS.containsKey(group)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown group '%s'. See /api/groups for valid options.".formatted(group));
        }

        log.info("Fetching TLE data from CelesTrak for group '{}'", group);

        try {
            String body = restClient.get()
                    .uri(CELESTRAK_URL, group)
                    .retrieve()
                    .body(String.class);

            List<OmmRecord> records = objectMapper.readValue(body, new TypeReference<>() {});

            if (records == null || records.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "CelesTrak returned no data for group '%s'.".formatted(group));
            }

            List<SpacecraftData> spacecraft = records.stream().map(CelestrakService::toSpacecraftData).toList();
            log.info("Cached {} spacecraft for group '{}'", spacecraft.size(), group);
            return spacecraft;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to fetch data from CelesTrak: " + e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OmmRecord(
            @JsonProperty("OBJECT_NAME") String objectName,
            @JsonProperty("OBJECT_ID") String objectId,
            @JsonProperty("NORAD_CAT_ID") Integer noradCatId,
            @JsonProperty("CLASSIFICATION_TYPE") String classificationType,
            @JsonProperty("EPOCH") String epoch,
            @JsonProperty("MEAN_MOTION") Double meanMotion,
            @JsonProperty("ECCENTRICITY") Double eccentricity,
            @JsonProperty("INCLINATION") Double inclination,
            @JsonProperty("RA_OF_ASC_NODE") Double raOfAscNode,
            @JsonProperty("ARG_OF_PERICENTER") Double argOfPericenter,
            @JsonProperty("MEAN_ANOMALY") Double meanAnomaly,
            @JsonProperty("EPHEMERIS_TYPE") Integer ephemerisType,
            @JsonProperty("ELEMENT_SET_NO") Integer elementSetNo,
            @JsonProperty("REV_AT_EPOCH") Integer revAtEpoch,
            @JsonProperty("BSTAR") Double bstar,
            @JsonProperty("MEAN_MOTION_DOT") Double meanMotionDot,
            @JsonProperty("MEAN_MOTION_DDOT") Double meanMotionDdot
    ) {}
}
