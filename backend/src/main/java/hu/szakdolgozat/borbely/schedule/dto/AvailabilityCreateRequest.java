package hu.szakdolgozat.borbely.schedule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Egy vagy több munkaidő-sáv felvétele egyszerre ({@code POST /api/barbers/{id}/availability}),
 * például egy egész hónapé. Vagy mind bekerül, vagy egyik sem.
 */
public record AvailabilityCreateRequest(
        @NotEmpty(message = "Legalább egy munkaidő-sávot meg kell adni.")
        @Size(max = 200, message = "Egyszerre legfeljebb 200 munkaidő-sáv adható meg.")
        List<@NotNull @Valid AvailabilitySlotRequest> slots
) {
}
