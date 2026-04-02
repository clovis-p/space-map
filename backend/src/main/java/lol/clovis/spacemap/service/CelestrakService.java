package lol.clovis.spacemap.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class CelestrakService {

    private static final Logger log = LoggerFactory.getLogger(CelestrakService.class);

    private static final String CELESTRAK_URL =
            "https://celestrak.org/NORAD/elements/gp.php?GROUP={group}&FORMAT=json";

    // Allowlist: protects against SSRF if the group param is ever used in a URL
    public static final Map<String, String> GROUPS = Map.ofEntries(
            Map.entry("stations", "Space Stations"),
            Map.entry("visual", "Brightest / Visible"),
            Map.entry("active", "All Active"),
            Map.entry("gps-ops", "GPS"),
            Map.entry("starlink", "Starlink"),
            Map.entry("weather", "Weather"),
            Map.entry("noaa", "NOAA"),
            Map.entry("goes", "GOES"),
            Map.entry("amateur", "Amateur Radio"),
            Map.entry("science", "Science"),
            Map.entry("iridium-next", "Iridium NEXT"),
            Map.entry("oneweb", "OneWeb"),
            Map.entry("galileo", "Galileo"),
            Map.entry("beidou", "BeiDou"),
            Map.entry("glonass", "GLONASS"),
            Map.entry("military", "Military"),
            Map.entry("cubesats", "CubeSats")
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

    private static final double MU_EARTH = 398600.4418; // km³/s²
    private static final double AU_IN_KM = 149597870.7;

    private static SatelliteData toSatelliteData(OmmRecord r) {
        double n = r.meanMotion() * 2 * Math.PI / 86400; // rad/s
        double semiMajorAxisAu = Math.pow(MU_EARTH / (n * n), 1.0 / 3.0) / AU_IN_KM;
        SatelliteData.OrbitalElements elements = new SatelliteData.OrbitalElements(
                semiMajorAxisAu,
                r.eccentricity(),
                r.inclination(),
                r.raOfAscNode(),
                r.argOfPericenter(),
                r.meanAnomaly(),
                r.meanMotion() * 360,
                r.epoch()
        );
        return new SatelliteData(String.valueOf(r.noradCatId()), r.objectName(), "earth", elements);
    }

    @Cacheable(value = "tle", key = "#group")
    public List<SatelliteData> fetchGroup(String group) {
        if (!GROUPS.containsKey(group)) {
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

            List<SatelliteData> satellites = records.stream().map(CelestrakService::toSatelliteData).toList();
            log.info("Cached {} satellites for group '{}'", satellites.size(), group);
            return satellites;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to fetch data from CelesTrak: " + e.getMessage());
        }
    }

    public record SatelliteData(
            String id,
            String name,
            String centralBodyId,
            OrbitalElements elements
    ) {
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
