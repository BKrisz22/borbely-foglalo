package hu.szakdolgozat.borbely.schedule.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Egy hét munkaidő-sávjainak átmásolása egy másik hétre
 * ({@code POST /api/barbers/{id}/availability/copy-week}). Mindkét dátum egy hétfő.
 */
public record CopyWeekRequest(
        @NotNull(message = "A forráshét megadása kötelező.")
        LocalDate sourceWeekStart,

        @NotNull(message = "A célhét megadása kötelező.")
        LocalDate targetWeekStart
) {
}
