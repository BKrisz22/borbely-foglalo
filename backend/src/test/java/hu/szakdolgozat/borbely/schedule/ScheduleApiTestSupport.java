package hu.szakdolgozat.borbely.schedule;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A beosztás-tesztek közös előkészítése. Minden teszt üres adatbázisból indul, két borbéllyal
 * (1: Kovács Péter, 2: Szabó Anna), és az 1-es borbély valódi tokenjével. A dátumok a következő
 * hét hétfőjéhez képest vannak megadva, így a tesztek bármelyik napon futtathatók.
 */
abstract class ScheduleApiTestSupport {

    static final ZoneId BUDAPEST = ZoneId.of("Europe/Budapest");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    /** A következő hét hétfője. */
    LocalDate monday;

    private String barberToken;

    @BeforeEach
    void prepareBarbers() throws Exception {
        jdbc.execute("TRUNCATE booking, time_off, availability, barber_service, barber, app_user, service_offering "
                + "RESTART IDENTITY CASCADE");
        monday = LocalDate.now(BUDAPEST).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        createBarber("kovacs.peter@example.com", "Kovács Péter");
        createBarber("szabo.anna@example.com", "Szabó Anna");
        barberToken = login("kovacs.peter@example.com", "Kezdo1234");
    }

    /** Az 1-es borbély (Kovács Péter) valódi tokenje. */
    RequestPostProcessor asBarber() {
        return request -> {
            request.addHeader("Authorization", "Bearer " + barberToken);
            return request;
        };
    }

    static RequestPostProcessor owner() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_OWNER"));
    }

    /** Egy helyi (budapesti) időpont időzónával, ahogy a felület küldené. */
    static OffsetDateTime at(LocalDate day, String time) {
        return day.atTime(LocalTime.parse(time)).atZone(BUDAPEST).toOffsetDateTime();
    }

    /** Regisztrál egy vendéget, és visszaadja a tokenjét. */
    String registerCustomer() throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "vendeg@example.com", "fullName": "Teszt Vendég", "password": "Titkos123"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.token");
    }

    /**
     * Közvetlenül az adatbázisba ír egy foglalást, mert a foglalási végpont még nem készült el.
     * Létrehoz hozzá egy szolgáltatást és egy vendéget is, ezért tesztenként egyszer hívható.
     */
    void book(long barberId, LocalDate day, String start, String end) throws Exception {
        mockMvc.perform(post("/api/services")
                        .with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Hajvágás", "durationMinutes": 30, "priceHuf": 5000}
                                """))
                .andExpect(status().isCreated());
        registerCustomer();
        Long customerId = jdbc.queryForObject(
                "SELECT id FROM app_user WHERE email = 'vendeg@example.com'", Long.class);
        jdbc.update("""
                INSERT INTO booking (barber_id, customer_id, service_offering_id, start_at, end_at, price_huf)
                VALUES (?, ?, 1, ?, ?, 5000)
                """, barberId, customerId, at(day, start), at(day, end));
    }

    private void createBarber(String email, String fullName) throws Exception {
        mockMvc.perform(post("/api/barbers")
                        .with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "fullName": "%s", "initialPassword": "Kezdo1234", "serviceIds": []}
                                """.formatted(email, fullName)))
                .andExpect(status().isCreated());
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
}
