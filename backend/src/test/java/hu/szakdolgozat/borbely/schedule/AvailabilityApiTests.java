package hu.szakdolgozat.borbely.schedule;

import hu.szakdolgozat.borbely.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A munkaidő-sávok integrációs tesztjei: jogosultság, átfedés, „mindent vagy semmit” mentés,
 * a foglalások védelme és az előző hét másolása.
 */
@IntegrationTest
class AvailabilityApiTests extends ScheduleApiTestSupport {

    @Test
    void barberOpensOwnDaysAndSeesThemInOrder() throws Exception {
        mockMvc.perform(createSlots(1, slot(monday, "14:00", "18:00"), slot(monday, "09:00", "12:00")).with(asBarber()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(listWeek(1, monday).with(asBarber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].workDate").value(monday.toString()))
                .andExpect(jsonPath("$[0].startTime").value(startsWith("09:00")))
                .andExpect(jsonPath("$[1].startTime").value(startsWith("14:00")));
    }

    @Test
    void scheduleIsNotPublic() throws Exception {
        mockMvc.perform(listWeek(1, monday))
                .andExpect(status().isUnauthorized());

        String customerToken = registerCustomer();
        mockMvc.perform(listWeek(1, monday).header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void barberCannotEditAnotherBarbersSchedule() throws Exception {
        mockMvc.perform(createSlots(2, slot(monday, "09:00", "12:00")).with(asBarber()))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanEditAnyBarbersSchedule() throws Exception {
        mockMvc.perform(createSlots(2, slot(monday, "09:00", "12:00")).with(owner()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].barberId").value(2));
    }

    @Test
    void unknownBarberReturns404() throws Exception {
        mockMvc.perform(listWeek(99, monday).with(owner()))
                .andExpect(status().isNotFound());
    }

    @Test
    void overlapWithExistingSlotIsRejectedAndNothingIsSaved() throws Exception {
        mockMvc.perform(createSlots(1, slot(monday, "09:00", "12:00")).with(asBarber()))
                .andExpect(status().isCreated());

        mockMvc.perform(createSlots(1, slot(monday.plusDays(1), "09:00", "12:00"), slot(monday, "11:00", "13:00"))
                        .with(asBarber()))
                .andExpect(status().isConflict());

        mockMvc.perform(listWeek(1, monday).with(asBarber()))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void overlapWithinOneRequestIsRejected() throws Exception {
        mockMvc.perform(createSlots(1, slot(monday, "09:00", "12:00"), slot(monday, "11:30", "13:00"))
                        .with(asBarber()))
                .andExpect(status().isConflict());
    }

    @Test
    void adjacentSlotsAreAllowed() throws Exception {
        mockMvc.perform(createSlots(1, slot(monday, "09:00", "12:00"), slot(monday, "12:00", "14:00"))
                        .with(asBarber()))
                .andExpect(status().isCreated());
    }

    @Test
    void invalidSlotsAreRejected() throws Exception {
        mockMvc.perform(createSlots(1, slot(monday, "09:10", "12:00")).with(asBarber()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(createSlots(1, slot(monday, "12:00", "09:00")).with(asBarber()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(createSlots(1, slot(LocalDate.now(BUDAPEST).minusDays(1), "09:00", "12:00")).with(asBarber()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(createSlots(1).with(asBarber()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void copyWeekCopiesTheSlotsOneWeekLater() throws Exception {
        mockMvc.perform(createSlots(1, slot(monday, "09:00", "12:00"), slot(monday.plusDays(2), "10:00", "14:00"))
                        .with(asBarber()))
                .andExpect(status().isCreated());

        mockMvc.perform(copyWeek(1, monday, monday.plusWeeks(1)).with(asBarber()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(listWeek(1, monday.plusWeeks(1)).with(asBarber()))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].workDate").value(monday.plusWeeks(1).toString()))
                .andExpect(jsonPath("$[1].workDate").value(monday.plusWeeks(1).plusDays(2).toString()))
                .andExpect(jsonPath("$[1].startTime").value(startsWith("10:00")));
    }

    @Test
    void copyWeekNeedsMondaysAndASourceWeekWithSlots() throws Exception {
        mockMvc.perform(copyWeek(1, monday.plusDays(1), monday.plusDays(8)).with(asBarber()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(copyWeek(1, monday, monday.plusWeeks(1)).with(asBarber()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void slotWithBookingCannotBeDeletedOrShrunkButCanBeExtended() throws Exception {
        mockMvc.perform(createSlots(1, slot(monday, "09:00", "12:00")).with(asBarber()))
                .andExpect(status().isCreated());
        book(1, monday, "10:00", "10:30");

        mockMvc.perform(delete("/api/barbers/1/availability/1").with(asBarber()))
                .andExpect(status().isConflict());
        mockMvc.perform(updateSlot(1, 1, "09:00", "10:00").with(asBarber()))
                .andExpect(status().isConflict());
        mockMvc.perform(updateSlot(1, 1, "09:00", "13:00").with(asBarber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endTime").value(startsWith("13:00")));
    }

    @Test
    void freeSlotCanBeDeleted() throws Exception {
        mockMvc.perform(createSlots(1, slot(monday, "09:00", "12:00")).with(asBarber()))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/barbers/1/availability/1").with(asBarber()))
                .andExpect(status().isNoContent());

        mockMvc.perform(listWeek(1, monday).with(asBarber()))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private static String slot(LocalDate day, String start, String end) {
        return """
                {"workDate": "%s", "startTime": "%s", "endTime": "%s"}""".formatted(day, start, end);
    }

    private static MockHttpServletRequestBuilder createSlots(long barberId, String... slots) {
        return post("/api/barbers/{id}/availability", barberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slots\": [" + Arrays.stream(slots).collect(Collectors.joining(", ")) + "]}");
    }

    private static MockHttpServletRequestBuilder updateSlot(long barberId, long slotId, String start, String end) {
        return put("/api/barbers/{id}/availability/{slotId}", barberId, slotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"startTime": "%s", "endTime": "%s"}
                        """.formatted(start, end));
    }

    private static MockHttpServletRequestBuilder copyWeek(long barberId, LocalDate source, LocalDate target) {
        return post("/api/barbers/{id}/availability/copy-week", barberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sourceWeekStart": "%s", "targetWeekStart": "%s"}
                        """.formatted(source, target));
    }

    private static MockHttpServletRequestBuilder listWeek(long barberId, LocalDate weekStart) {
        return get("/api/barbers/{id}/availability", barberId)
                .param("from", weekStart.toString())
                .param("to", weekStart.plusDays(6).toString());
    }
}
