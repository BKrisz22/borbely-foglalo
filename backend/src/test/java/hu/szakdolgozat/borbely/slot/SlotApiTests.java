package hu.szakdolgozat.borbely.slot;

import com.jayway.jsonpath.JsonPath;
import hu.szakdolgozat.borbely.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A szabad kezdőpontok és a foglalható napok végpontjainak integrációs tesztjei. Minden teszt
 * üres adatbázisból indul: két szolgáltatás (1: 30 perc, 2: 40 perc), egy borbély, aki mindkettőt
 * vállalja, és egy vendég. A végpontokat bejelentkezés nélkül hívják, ahogy a foglalási tölcsér.
 */
@IntegrationTest
class SlotApiTests {

    private static final ZoneId BUDAPEST = ZoneId.of("Europe/Budapest");
    private static final long HAIRCUT = 1;
    private static final long HAIRCUT_AND_BEARD = 2;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    /** A következő hét hétfője. */
    private LocalDate monday;

    @BeforeEach
    void prepare() throws Exception {
        jdbc.execute("TRUNCATE booking, time_off, availability, barber_service, barber, app_user, service_offering "
                + "RESTART IDENTITY CASCADE");
        monday = LocalDate.now(BUDAPEST).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        createService("Hajvágás", 30);
        createService("Hajvágás és szakáll", 40);
        mockMvc.perform(post("/api/barbers")
                        .with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "kovacs.peter@example.com", "fullName": "Kovács Péter",
                                 "initialPassword": "Kezdo1234", "serviceIds": [1, 2]}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "vendeg@example.com", "fullName": "Teszt Vendég", "password": "Titkos123"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void slotsAroundABookingFollowTheDesignDocumentExample() throws Exception {
        open(monday, "09:00", "12:00");
        book(monday, "10:00", "10:30", "BOOKED");

        mockMvc.perform(slots(HAIRCUT_AND_BEARD, monday))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].time").value(contains("09:00", "09:15", "10:30", "10:45", "11:00", "11:15")))
                .andExpect(jsonPath("$[0].startAt").value(at(monday, "09:00").toInstant().toString()));
    }

    @Test
    void timeOffBlocksSlotsButACancelledBookingDoesNot() throws Exception {
        open(monday, "09:00", "12:00");
        book(monday, "09:00", "09:30", "CANCELLED");
        mockMvc.perform(post("/api/barbers/1/time-off")
                        .with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startAt": "%s", "endAt": "%s"}
                                """.formatted(at(monday, "11:30"), at(monday, "12:00"))))
                .andExpect(status().isCreated());

        mockMvc.perform(slots(HAIRCUT_AND_BEARD, monday))
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[0].time").value("09:00"))
                .andExpect(jsonPath("$[7].time").value("10:45"));
    }

    @Test
    void aBookingCannotSpanTheBreakBetweenTwoWorkingPeriods() throws Exception {
        open(monday, "09:00", "10:00", "10:15", "11:00");

        mockMvc.perform(slots(HAIRCUT_AND_BEARD, monday))
                .andExpect(jsonPath("$[*].time").value(contains("09:00", "09:15", "10:15")));
    }

    @Test
    void noSlotStartsWithinTheBarbersLeadTime() throws Exception {
        LocalDate today = LocalDate.now(BUDAPEST);
        open(today, "00:00", "23:45");
        Instant earliest = Instant.now().plus(Duration.ofMinutes(60));

        String body = mockMvc.perform(slots(HAIRCUT, today))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> starts = JsonPath.read(body, "$[*].startAt");

        assertThat(starts).allSatisfy(start -> assertThat(Instant.parse(start)).isAfterOrEqualTo(earliest));
        if (!starts.isEmpty()) {
            assertThat(Instant.parse(starts.getFirst())).isBefore(earliest.plus(Duration.ofMinutes(16)));
        }
    }

    @Test
    void thereAreNoSlotsOutsideTheBookingWindow() throws Exception {
        LocalDate tooFar = LocalDate.now(BUDAPEST).plusDays(31);
        open(tooFar, "09:00", "12:00");

        mockMvc.perform(slots(HAIRCUT, tooFar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
        mockMvc.perform(slots(HAIRCUT, LocalDate.now(BUDAPEST).minusDays(1)))
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void invalidRequestsAreRejected() throws Exception {
        createService("Borotválás", 20);

        mockMvc.perform(get("/api/barbers/99/slots").param("serviceId", "1").param("date", monday.toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(slots(3, monday))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/barbers/1/slots").param("date", monday.toString()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/barbers/1").with(owner())).andExpect(status().isNoContent());
        mockMvc.perform(slots(HAIRCUT, monday))
                .andExpect(status().isNotFound());
    }

    @Test
    void availableDaysListOnlyDaysWithAFreeStart() throws Exception {
        open(monday, "09:00", "12:00");
        open(monday.plusDays(2), "09:00", "09:30");
        open(monday.plusDays(4), "14:00", "16:00");

        // A szerdai fél óra a 40 perces szolgáltatásra kevés, a 30 percesre elég.
        mockMvc.perform(availableDays(HAIRCUT_AND_BEARD, monday, monday.plusDays(6)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(contains(monday.toString(), monday.plusDays(4).toString())));
        mockMvc.perform(availableDays(HAIRCUT, monday, monday.plusDays(6)))
                .andExpect(jsonPath("$").value(contains(
                        monday.toString(), monday.plusDays(2).toString(), monday.plusDays(4).toString())));
    }

    /** Munkaidő-sávok egy napra, időpontpárokban: open(nap, "09:00", "12:00", "14:00", "18:00"). */
    private void open(LocalDate day, String... times) throws Exception {
        StringBuilder slots = new StringBuilder();
        for (int i = 0; i < times.length; i += 2) {
            if (!slots.isEmpty()) {
                slots.append(", ");
            }
            slots.append("""
                    {"workDate": "%s", "startTime": "%s", "endTime": "%s"}""".formatted(day, times[i], times[i + 1]));
        }
        mockMvc.perform(post("/api/barbers/1/availability")
                        .with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slots\": [" + slots + "]}"))
                .andExpect(status().isCreated());
    }

    /** Foglalás közvetlenül az adatbázisba, mert a foglalási végpont még nem készült el. */
    private void book(LocalDate day, String start, String end, String status) {
        Long customerId = jdbc.queryForObject(
                "SELECT id FROM app_user WHERE email = 'vendeg@example.com'", Long.class);
        jdbc.update("""
                INSERT INTO booking (barber_id, customer_id, service_offering_id, start_at, end_at, price_huf,
                                     status, cancelled_at)
                VALUES (1, ?, 1, ?, ?, 5000, ?, CASE WHEN ? = 'CANCELLED' THEN now() END)
                """, customerId, at(day, start), at(day, end), status, status);
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

    private static MockHttpServletRequestBuilder slots(long serviceId, LocalDate date) {
        return get("/api/barbers/1/slots")
                .param("serviceId", String.valueOf(serviceId))
                .param("date", date.toString());
    }

    private static MockHttpServletRequestBuilder availableDays(long serviceId, LocalDate from, LocalDate to) {
        return get("/api/barbers/1/available-days")
                .param("serviceId", String.valueOf(serviceId))
                .param("from", from.toString())
                .param("to", to.toString());
    }

    private static OffsetDateTime at(LocalDate day, String time) {
        return day.atTime(LocalTime.parse(time)).atZone(BUDAPEST).toOffsetDateTime();
    }

    private static RequestPostProcessor owner() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_OWNER"));
    }
}
