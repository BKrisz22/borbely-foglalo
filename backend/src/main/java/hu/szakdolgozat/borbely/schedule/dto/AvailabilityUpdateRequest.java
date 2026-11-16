package hu.szakdolgozat.borbely.schedule.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * Egy munkaidő-sáv idejének módosítása ({@code PUT /api/barbers/{id}/availability/{slotId}}).
 * A nap nem változik; másik napra törléssel és új sávval lehet áttenni.
 */
public record AvailabilityUpdateRequest(
        @NotNull(message = "A kezdés megadása kötelező.")
        LocalTime startTime,

        @NotNull(message = "A befejezés megadása kötelező.")
        LocalTime endTime
) {
}
