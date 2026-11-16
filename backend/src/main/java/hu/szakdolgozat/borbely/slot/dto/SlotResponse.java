package hu.szakdolgozat.borbely.slot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.time.LocalTime;

/**
 * Egy foglalható kezdőpont. A {@code startAt} UTC-ben van, ezt kell foglaláskor visszaküldeni;
 * a {@code time} ugyanez budapesti időben, megjelenítéshez (például „09:15”).
 */
public record SlotResponse(
        Instant startAt,

        @JsonFormat(pattern = "HH:mm")
        LocalTime time
) {
}
