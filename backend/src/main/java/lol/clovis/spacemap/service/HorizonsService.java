package lol.clovis.spacemap.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HorizonsService {

    private static final Logger log = LoggerFactory.getLogger(HorizonsService.class);

    // Ordered Mercury to Neptune; Sun is excluded (no heliocentric elements needed)
    private static final Map<String, String> PLANET_IDS = new LinkedHashMap<>(Map.of(
            "mercury", "199",
            "venus", "299",
            "earth", "399",
            "mars", "499",
            "jupiter", "599",
            "saturn", "699",
            "uranus", "799",
            "neptune", "899"
    ));

    private static final DateTimeFormatter HORIZONS_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss.SSSS", Locale.ENGLISH);

    private static final Pattern FIELD_PATTERN =
            Pattern.compile("\\b(EC|A|IN|OM|W|MA|N)\\s*=\\s*([\\d.E+\\-]+)");

    private static final Pattern EPOCH_PATTERN =
            Pattern.compile("A\\.D\\.\\s+(\\d{4}-\\w{3}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d+)\\s+TDB");

    // Fallback: J2000 mean elements from NASA JPL https://ssd.jpl.nasa.gov/planets/approx_pos.html
    private static final List<Body> FALLBACK_BODIES = List.of(
            new Body("sun", "Sun", 696000, 0xffd700, null, null),
            new Body("mercury", "Mercury", 2439.7, 0xb5b5b5, new OrbitalElements(0.38709927, 0.20563593, 7.00497902, 48.33076593, 29.12703035, 174.79252722, 4.09233445, null), "fallback"),
            new Body("venus", "Venus", 6051.8, 0xe8cda0, new OrbitalElements(0.72333566, 0.00677672, 3.39467605, 76.67984255, 54.92262463, 50.37663232, 1.60213034, null), "fallback"),
            new Body("earth", "Earth", 6371.0, 0x4fc3f7, new OrbitalElements(1.00000261, 0.01671123, -0.00001531, 0.0, 102.93768193, 100.46457166, 0.98560028, null), "fallback"),
            new Body("mars", "Mars", 3389.5, 0xc1440e, new OrbitalElements(1.52371034, 0.09339410, 1.84969142, 49.55953891, 286.49748, 355.44306, 0.52402068, null), "fallback"),
            new Body("jupiter", "Jupiter", 69911.0, 0xc88b3a, new OrbitalElements(5.20288700, 0.04838624, 1.30439695, 100.47390909, 273.86712, 34.39644051, 0.08308676, null), "fallback"),
            new Body("saturn", "Saturn", 58232.0, 0xe4d191, new OrbitalElements(9.53667594, 0.05386179, 2.48599187, 113.66242448, 339.39149, 49.95424423, 0.03344414, null), "fallback"),
            new Body("uranus", "Uranus", 25362.0, 0x7de8e8, new OrbitalElements(19.18916464, 0.04725744, 0.77263783, 74.01692503, 96.99895829, 313.23810451, 0.01172834, null), "fallback"),
            new Body("neptune", "Neptune", 24622.0, 0x3f54ba, new OrbitalElements(30.06992276, 0.00859048, 1.77004347, 131.78422574, 276.34, 304.88003, 0.00598103, null), "fallback")
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String horizonsUrl;

    public HorizonsService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper,
                           @Value("${horizons.api.url}") String horizonsUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = restClientBuilder.requestFactory(factory).build();
        this.objectMapper = objectMapper;
        this.horizonsUrl = horizonsUrl;
    }

    @Cacheable("planets")
    public List<Body> getAllBodies() {
        try {
            Map<String, OrbitalElements> liveElements = fetchAllElements();
            return FALLBACK_BODIES.stream()
                    .map(body -> {
                        if (body.elements() == null) return body;
                        OrbitalElements live = liveElements.get(body.id());
                        if (live != null) {
                            return new Body(body.id(), body.name(), body.radiusKm(), body.color(), live, "horizons");
                        }
                        return body;
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("Horizons fetch failed, using fallback elements: {}", e.getMessage());
            return FALLBACK_BODIES;
        }
    }

    private Map<String, OrbitalElements> fetchAllElements() throws Exception {
        List<CompletableFuture<Map.Entry<String, OrbitalElements>>> futures = PLANET_IDS.entrySet()
                .stream()
                .map(e -> fetchElementsAsync(e.getValue())
                .thenApply(elements -> Map.entry(e.getKey(), elements)))
                .toList();

        Map<String, OrbitalElements> result = new LinkedHashMap<>();
        for (var future : futures) {
            var entry = future.join();
            result.put(entry.getKey(), entry.getValue());
        }
        log.info("Fetched live orbital elements for {} planets from JPL Horizons", result.size());
        return result;
    }

    @Async
    public CompletableFuture<OrbitalElements> fetchElementsAsync(String horizonsId) {
        return CompletableFuture.completedFuture(fetchElements(horizonsId));
    }

    private OrbitalElements fetchElements(String horizonsId) {
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        String tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1).toString();

        // Horizons requires STOP_TIME > START_TIME, so we request a 1-day window and take the first row
        String url = horizonsUrl
                + "?format=json&COMMAND=" + horizonsId
                + "&CENTER=500%4010"
                + "&EPHEM_TYPE=ELEMENTS"
                + "&OUT_UNITS=AU-D"
                + "&OBJ_DATA=NO"
                + "&MAKE_EPHEM=YES"
                + "&START_TIME=" + today
                + "&STOP_TIME=" + tomorrow
                + "&STEP_SIZE=1d";

        String raw = restClient.get().uri(URI.create(url)).retrieve().body(String.class);
        HorizonsResponse response = parseJson(raw);
        return parseElements(response.result());
    }

    // Package-private for unit testing
    static OrbitalElements parseElements(String result) {
        int soeIdx = result.indexOf("$$SOE");
        int eoeIdx = result.indexOf("$$EOE");
        if (soeIdx == -1 || eoeIdx == -1) {
            throw new IllegalArgumentException("Horizons response missing $$SOE/$$EOE markers");
        }
        String block = result.substring(soeIdx, eoeIdx);

        Matcher epochMatcher = EPOCH_PATTERN.matcher(block);
        if (!epochMatcher.find()) {
            throw new IllegalArgumentException("Could not parse epoch from Horizons response");
        }
        String epochIso = LocalDateTime.parse(epochMatcher.group(1).trim(), HORIZONS_DATE_FMT)
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        Map<String, Double> fields = new LinkedHashMap<>();
        Matcher m = FIELD_PATTERN.matcher(block);
        while (m.find()) {
            fields.putIfAbsent(m.group(1), Double.parseDouble(m.group(2)));
        }

        for (String required : List.of("EC", "A", "IN", "OM", "W", "MA", "N")) {
            if (!fields.containsKey(required)) {
                throw new IllegalArgumentException("Horizons response missing field: " + required);
            }
        }

        return new OrbitalElements(
                fields.get("A"),
                fields.get("EC"),
                fields.get("IN"),
                fields.get("OM"),
                fields.get("W"),
                fields.get("MA"),
                fields.get("N"),
                epochIso
        );
    }

    private HorizonsResponse parseJson(String raw) {
        try {
            return objectMapper.readValue(raw, HorizonsResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Horizons JSON response: " + e.getMessage(), e);
        }
    }

    private record HorizonsResponse(@JsonProperty("result") String result) {}

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

    public record Body(
            String id,
            String name,
            double radiusKm,
            int color,
            OrbitalElements elements,
            String source
    ) {}
}
