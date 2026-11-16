package hu.szakdolgozat.borbely.schedule.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Egy munkaidő-sáv egy napon, helyi idő szerint (például 2026-12-01, 09:00–12:00).
 */
public record AvailabilitySlotRequest(
        @NotNull(message = "A nap megadása kötelező.")
        LocalDate workDate,

        @NotNull(message = "A kezdés megadása kötelező.")
        LocalTime startTime,

        @NotNull(message = "A befejezés megadása kötelező.")
        LocalTime endTime
) {
}
