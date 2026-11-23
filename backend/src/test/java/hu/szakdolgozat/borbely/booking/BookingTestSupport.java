package hu.szakdolgozat.borbely.booking;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A foglalási tesztek közös előkészítése. Minden teszt üres adatbázisból indul:
 * két szolgáltatás (1: Hajvágás 30 perc, 2: Szakáll 40 perc), két borbély, aki mindkettőt vállalja
 * (1: Kovács Péter, 2: Szabó Anna), és két vendég (Vendég Vilma, Vendég Béla), mindenki valódi tokennel.
 * A {@code day} egy hét múlva van, így a 24 órás lemondási határidőn és a 30 napos ablakon belül marad.
 */
abstract class BookingTestSupport {

    static final ZoneId BUDAPEST = ZoneId.of("Europe/Budapest");
    static final long HAIRCUT = 1;
    static final long BEARD = 2;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    LocalDate day;
    String barber1Token;
    String barber2Token;
    String vilmaToken;
    String belaToken;
    long vilmaId;
    long belaId;

    @BeforeEach
    void prepareShop() throws Exception {
        jdbc.execute("TRUNCATE booking, time_off, availability, barber_service, barber, app_user, service_offering "
                + "RESTART IDENTITY CASCADE");
        day = LocalDate.now(BUDAPEST).plusDays(7);
        createService("Hajvágás", 30);
        createService("Szakáll", 40);
        createBarber("kovacs.peter@example.com", "Kovács Péter");
        createBarber("szabo.anna@example.com", "Szabó Anna");
        barber1Token = login("kovacs.peter@example.com", "Kezdo1234");
        barber2Token = login("szabo.anna@example.com", "Kezdo1234");
        vilmaToken = register("vilma@example.com", "Vendég Vilma");
        belaToken = register("bela@example.com", "Vendég Béla");
        vilmaId = userId("vilma@example.com");
        belaId = userId("bela@example.com");
    }

    /** Munkaidő nyitása a tulajdonos nevében. */
    void open(long barberId, LocalDate date, String start, String end) throws Exception {
        mockMvc.perform(post("/api/barbers/{id}/availability", barberId)
                        .with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slots": [{"workDate": "%s", "startTime": "%s", "endTime": "%s"}]}
                                """.formatted(date, start, end)))
                .andExpect(status().isCreated());
    }

    /** Foglalási kérés; a {@code customerId} null is lehet. A hitelesítést a hívó teszi hozzá. */
    static MockHttpServletRequestBuilder book(long barberId, long serviceId, OffsetDateTime startAt, Long customerId) {
        return post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"barberId": %d, "serviceId": %d, "startAt": "%s", "customerId": %s}
                        """.formatted(barberId, serviceId, startAt, customerId));
    }

    /** Foglalás közvetlenül az adatbázisba, a szabályok megkerülésével (múltbeli vagy közeli kezdéshez). */
    long insertBooking(long barberId, long customerId, Instant startAt) {
        jdbc.update("""
                INSERT INTO booking (barber_id, customer_id, service_offering_id, start_at, end_at, price_huf)
                VALUES (?, ?, 1, ?, ?, 5000)
                """, barberId, customerId, startAt.atOffset(ZoneOffset.UTC),
                startAt.plusSeconds(30 * 60).atOffset(ZoneOffset.UTC));
        return jdbc.queryForObject("SELECT max(id) FROM booking", Long.class);
    }

    static OffsetDateTime at(LocalDate date, String time) {
        return date.atTime(LocalTime.parse(time)).atZone(BUDAPEST).toOffsetDateTime();
    }

    static RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    static RequestPostProcessor owner() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_OWNER"));
    }

    private void createService(String name, int durationMinutes) throws Exception {
        mockMvc.perform(post("/api/services")
                        .with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "durationMinutes": %d, "priceHuf": 5000}
                                """.formatted(name, durationMinutes)))
                .andExpect(status().isCreated());
    }

    private void createBarber(String email, String fullName) throws Exception {
        mockMvc.perform(post("/api/barbers")
                        .with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "fullName": "%s", "initialPassword": "Kezdo1234", "serviceIds": [1, 2]}
                                """.formatted(email, fullName)))
                .andExpect(status().isCreated());
    }

    private String register(String email, String fullName) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "fullName": "%s", "password": "Titkos123"}
                                """.formatted(email, fullName)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.token");
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.token");
    }

    private long userId(String email) {
        return jdbc.queryForObject("SELECT id FROM app_user WHERE email = ?", Long.class, email);
    }
}
