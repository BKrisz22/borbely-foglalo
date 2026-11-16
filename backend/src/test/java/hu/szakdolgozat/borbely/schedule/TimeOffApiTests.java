package hu.szakdolgozat.borbely.schedule;

import hu.szakdolgozat.borbely.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A távollétek integrációs tesztjei: felvétel és listázás, ütközés foglalással és másik
 * távolléttel, hibás időpontok, valamint hogy ki törölhet.
 */
@IntegrationTest
class TimeOffApiTests extends ScheduleApiTestSupport {

    @Test
    void barberRecordsTimeOffAndSeesIt() throws Exception {
        mockMvc.perform(createTimeOff(1, at(monday, "12:00"), at(monday, "13:00"), "  Orvos  ").with(asBarber()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reason").value("Orvos"))
                .andExpect(jsonPath("$.startAt").value(at(monday, "12:00").toInstant().toString()));

        mockMvc.perform(get("/api/barbers/1/time-off")
                        .param("from", monday.toString())
                        .param("to", monday.toString())
                        .with(asBarber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void timeOffOverABookingIsRejected() throws Exception {
        book(1, monday, "12:30", "13:00");

        mockMvc.perform(createTimeOff(1, at(monday, "12:00"), at(monday, "14:00"), null).with(asBarber()))
                .andExpect(status().isConflict());
    }

    @Test
    void overlappingTimeOffIsRejected() throws Exception {
        mockMvc.perform(createTimeOff(1, at(monday, "12:00"), at(monday, "14:00"), null).with(asBarber()))
                .andExpect(status().isCreated());

        mockMvc.perform(createTimeOff(1, at(monday, "13:00"), at(monday, "15:00"), null).with(asBarber()))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidTimeOffIsRejected() throws Exception {
        LocalDate yesterday = LocalDate.now(BUDAPEST).minusDays(1);
        mockMvc.perform(createTimeOff(1, at(monday, "12:10"), at(monday, "13:00"), null).with(asBarber()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(createTimeOff(1, at(monday, "13:00"), at(monday, "12:00"), null).with(asBarber()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(createTimeOff(1, at(yesterday, "09:00"), at(yesterday, "10:00"), null).with(asBarber()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void timeOffCanBeDeletedOnlyByItsBarberOrTheOwner() throws Exception {
        mockMvc.perform(createTimeOff(2, at(monday, "12:00"), at(monday, "13:00"), null).with(owner()))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/barbers/2/time-off/1").with(asBarber()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/barbers/1/time-off/1").with(asBarber()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/barbers/2/time-off/1").with(owner()))
                .andExpect(status().isNoContent());
    }

    private static MockHttpServletRequestBuilder createTimeOff(long barberId, Object start, Object end, String reason) {
        String reasonJson = reason == null ? "null" : "\"" + reason + "\"";
        return post("/api/barbers/{id}/time-off", barberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"startAt": "%s", "endAt": "%s", "reason": %s}
                        """.formatted(start, end, reasonJson));
    }
}
