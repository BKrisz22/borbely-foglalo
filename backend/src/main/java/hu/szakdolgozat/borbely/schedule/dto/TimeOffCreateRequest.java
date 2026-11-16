package hu.szakdolgozat.borbely.schedule.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Távollét felvétele ({@code POST /api/barbers/{id}/time-off}). Az időpontok időzónával érkeznek,
 * például {@code 2026-12-24T09:00+01:00}; az indoklás elhagyható.
 */
public record TimeOffCreateRequest(
        @NotNull(message = "A kezdés megadása kötelező.")
        OffsetDateTime startAt,

        @NotNull(message = "A befejezés megadása kötelező.")
        OffsetDateTime endAt,

        @Size(max = 200, message = "Az indoklás legfeljebb 200 karakter lehet.")
        String reason
) {
}
