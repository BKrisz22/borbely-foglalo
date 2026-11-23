package hu.szakdolgozat.borbely.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/**
 * Új foglalás ({@code POST /api/bookings}). A {@code startAt} a slot-végpont által visszaadott
 * kezdőpont, időzónával (például {@code 2026-12-07T09:00:00+01:00}). A {@code customerId} csak akkor
 * kell, ha a borbély vagy a tulajdonos foglal egy vendég nevében; a vendég mindig a saját nevére foglal.
 * Az árat és a befejezést a szerver számolja, a klienstől nem fogadja el.
 */
public record BookingCreateRequest(
        @NotNull(message = "A borbély megadása kötelező.")
        Long barberId,

        @NotNull(message = "A szolgáltatás megadása kötelező.")
        Long serviceId,

        @NotNull(message = "A kezdés megadása kötelező.")
        OffsetDateTime startAt,

        Long customerId
) {
}
