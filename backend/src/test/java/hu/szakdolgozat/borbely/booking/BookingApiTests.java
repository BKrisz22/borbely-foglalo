package hu.szakdolgozat.borbely.booking;

import hu.szakdolgozat.borbely.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assumptions.assumeThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A Foglalás API integrációs tesztjei: létrehozás és ütközések, foglalás a vendég nevében,
 * szerepkör szerinti listák és láthatóság, lemondás és lezárás.
 */
@IntegrationTest
class BookingApiTests extends BookingTestSupport {

    @Test
    void customerBooksAFreeSlotAndItDisappearsFromTheFreeSlots() throws Exception {
        open(1, day, "09:00", "12:00");

        mockMvc.perform(book(1, HAIRCUT, at(day, "09:00"), null).with(bearer(vilmaToken)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/bookings/1"))
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.customerName").value("Vendég Vilma"))
                .andExpect(jsonPath("$.barberName").value("Kovács Péter"))
                .andExpect(jsonPath("$.priceHuf").value(5000))
                .andExpect(jsonPath("$.endAt").value(at(day, "09:30").toInstant().toString()));

        mockMvc.perform(get("/api/barbers/1/slots").param("serviceId", "1").param("date", day.toString()))
                .andExpect(jsonPath("$[0].time").value("09:30"));
    }

    @Test
    void aTakenOrOverlappingSlotReturns409() throws Exception {
        open(1, day, "09:00", "12:00");
        mockMvc.perform(book(1, HAIRCUT, at(day, "09:00"), null).with(bearer(vilmaToken)))
                .andExpect(status().isCreated());

        mockMvc.perform(book(1, HAIRCUT, at(day, "09:00"), null).with(bearer(belaToken)))
                .andExpect(status().isConflict());
        mockMvc.perform(book(1, BEARD, at(day, "09:15"), null).with(bearer(belaToken)))
                .andExpect(status().isConflict());
    }

    @Test
    void aStartOutsideTheFreeSlotsReturns409() throws Exception {
        open(1, day, "09:00", "12:00");

        mockMvc.perform(book(1, HAIRCUT, at(day, "09:10"), null).with(bearer(vilmaToken)))
                .andExpect(status().isConflict());
        mockMvc.perform(book(1, HAIRCUT, at(day, "13:00"), null).with(bearer(vilmaToken)))
                .andExpect(status().isConflict());
        mockMvc.perform(book(1, HAIRCUT, at(LocalDate.now(BUDAPEST).plusDays(31), "09:00"), null)
                        .with(bearer(vilmaToken)))
                .andExpect(status().isConflict());
    }

    @Test
    void aCustomerCannotHaveTwoOverlappingBookingsEvenAtDifferentBarbers() throws Exception {
        open(1, day, "09:00", "12:00");
        open(2, day, "09:00", "12:00");
        mockMvc.perform(book(1, HAIRCUT, at(day, "09:00"), null).with(bearer(vilmaToken)))
                .andExpect(status().isCreated());

        mockMvc.perform(book(2, HAIRCUT, at(day, "09:15"), null).with(bearer(vilmaToken)))
                .andExpect(status().isConflict());
        // Közvetlenül utána (9:30-tól) már foglalhat.
        mockMvc.perform(book(2, HAIRCUT, at(day, "09:30"), null).with(bearer(vilmaToken)))
                .andExpect(status().isCreated());
    }

    @Test
    void aCustomerCanOnlyBookForThemselves() throws Exception {
        open(1, day, "09:00", "12:00");

        mockMvc.perform(book(1, HAIRCUT, at(day, "09:00"), belaId).with(bearer(vilmaToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(book(1, HAIRCUT, at(day, "09:00"), vilmaId).with(bearer(vilmaToken)))
                .andExpect(status().isCreated());
    }

    @Test
    void aBarberBooksForACustomerOnlyInTheirOwnCalendar() throws Exception {
        open(1, day, "09:00", "12:00");
        open(2, day, "09:00", "12:00");

        mockMvc.perform(book(1, HAIRCUT, at(day, "09:00"), belaId).with(bearer(barber1Token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Vendég Béla"));
        mockMvc.perform(book(2, HAIRCUT, at(day, "10:00"), belaId).with(bearer(barber1Token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(book(1, HAIRCUT, at(day, "10:00"), null).with(bearer(barber1Token)))
                .andExpect(status().isBadRequest());
        // A másik borbély fiókja nem vendég, az ő nevére nem lehet foglalni.
        long barber2UserId = jdbc.queryForObject("SELECT user_id FROM barber WHERE id = 2", Long.class);
        mockMvc.perform(book(1, HAIRCUT, at(day, "10:00"), barber2UserId).with(bearer(barber1Token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void staffCanBookInsideTheLeadTimeButTheCustomerCannot() throws Exception {
        LocalDate today = LocalDate.now(BUDAPEST);
        OffsetDateTime soon = nextQuarterAfter(Instant.now().plus(Duration.ofMinutes(20)));
        assumeThat(soon.atZoneSameInstant(BUDAPEST).toLocalDate()).isEqualTo(today);
        assumeThat(soon.atZoneSameInstant(BUDAPEST).getHour()).isLessThan(23);
        open(1, today, "00:00", "23:45");

        // A borbély foglalási határideje 60 perc, ezért a vendég 20 percen belüli kezdést nem foglalhat.
        mockMvc.perform(book(1, HAIRCUT, soon, null).with(bearer(vilmaToken)))
                .andExpect(status().isConflict());
        mockMvc.perform(book(1, HAIRCUT, soon, vilmaId).with(owner()))
                .andExpect(status().isCreated());
    }

    @Test
    void listsAreScopedByRole() throws Exception {
        open(1, day, "09:00", "12:00");
        open(2, day, "09:00", "12:00");
        mockMvc.perform(book(1, HAIRCUT, at(day, "09:00"), null).with(bearer(vilmaToken)))
                .andExpect(status().isCreated());
        mockMvc.perform(book(2, HAIRCUT, at(day, "09:00"), null).with(bearer(belaToken)))
                .andExpect(status().isCreated());

        mockMvc.perform(list(day).with(bearer(vilmaToken)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerName").value("Vendég Vilma"));
        mockMvc.perform(list(day).with(bearer(barber1Token)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].barberId").value(1));
        mockMvc.perform(list(day).with(owner()))
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(list(day).param("barberId", "2").with(owner()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerName").value("Vendég Béla"));
        mockMvc.perform(list(day).param("barberId", "2").with(bearer(barber1Token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyTheCustomerTheBarberAndTheOwnerSeeABooking() throws Exception {
        open(1, day, "09:00", "12:00");
        mockMvc.perform(book(1, HAIRCUT, at(day, "09:00"), null).with(bearer(vilmaToken)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/bookings/1").with(bearer(vilmaToken))).andExpect(status().isOk());
        mockMvc.perform(get("/api/bookings/1").with(bearer(barber1Token))).andExpect(status().isOk());
        mockMvc.perform(get("/api/bookings/1").with(owner())).andExpect(status().isOk());
        mockMvc.perform(get("/api/bookings/1").with(bearer(belaToken))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/bookings/1").with(bearer(barber2Token))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/bookings/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void theCustomerCancelsWellBeforeTheStartAndTheSlotIsFreeAgain() throws Exception {
        open(1, day, "09:00", "12:00");
        mockMvc.perform(book(1, HAIRCUT, at(day, "09:00"), null).with(bearer(vilmaToken)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/bookings/1/cancel").with(bearer(vilmaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").exists());
        mockMvc.perform(post("/api/bookings/1/cancel").with(bearer(vilmaToken)))
                .andExpect(status().isConflict());
        mockMvc.perform(book(1, HAIRCUT, at(day, "09:00"), null).with(bearer(belaToken)))
                .andExpect(status().isCreated());
    }

    @Test
    void within24HoursOnlyTheBarberCanCancel() throws Exception {
        long id = insertBooking(1, vilmaId, Instant.now().plus(Duration.ofHours(2)).truncatedTo(ChronoUnit.MINUTES));

        mockMvc.perform(post("/api/bookings/{id}/cancel", id).with(bearer(vilmaToken)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/bookings/{id}/cancel", id).with(bearer(barber1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void onlyStaffClosesABookingAndOnlyAfterItStarted() throws Exception {
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1)).truncatedTo(ChronoUnit.MINUTES);
        long past = insertBooking(1, vilmaId, oneHourAgo);
        long pastToo = insertBooking(1, belaId, oneHourAgo.minus(Duration.ofHours(1)));
        long future = insertBooking(1, vilmaId, Instant.now().plus(Duration.ofDays(2)).truncatedTo(ChronoUnit.MINUTES));

        mockMvc.perform(post("/api/bookings/{id}/complete", past).with(bearer(vilmaToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/bookings/{id}/complete", past).with(bearer(barber1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mockMvc.perform(post("/api/bookings/{id}/cancel", past).with(bearer(barber1Token)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/bookings/{id}/no-show", pastToo).with(owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"));
        mockMvc.perform(post("/api/bookings/{id}/complete", future).with(bearer(barber1Token)))
                .andExpect(status().isConflict());
    }

    @Test
    void bookingRequiresLogin() throws Exception {
        open(1, day, "09:00", "12:00");

        mockMvc.perform(book(1, HAIRCUT, at(day, "09:00"), null))
                .andExpect(status().isUnauthorized());
    }

    private static MockHttpServletRequestBuilder list(LocalDate date) {
        return get("/api/bookings").param("from", date.toString()).param("to", date.toString());
    }

    /** Az adott időpont utáni első negyedóra, budapesti időben. */
    private static OffsetDateTime nextQuarterAfter(Instant instant) {
        long quarter = 15 * 60;
        long seconds = (instant.getEpochSecond() / quarter + 1) * quarter;
        return Instant.ofEpochSecond(seconds).atZone(BUDAPEST).toOffsetDateTime();
    }
}
