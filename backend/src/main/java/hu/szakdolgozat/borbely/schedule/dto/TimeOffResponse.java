package hu.szakdolgozat.borbely.schedule.dto;

import hu.szakdolgozat.borbely.schedule.TimeOff;

import java.time.Instant;

/**
 * Egy távollét. Az időpontok UTC-ben mennek ki; a felület alakítja budapesti időre.
 */
public record TimeOffResponse(
        Long id,
        Long barberId,
        Instant startAt,
        Instant endAt,
        String reason
) {

    public static TimeOffResponse from(TimeOff timeOff) {
        return new TimeOffResponse(
                timeOff.getId(),
                timeOff.getBarber().getId(),
                timeOff.getStartAt(),
                timeOff.getEndAt(),
                timeOff.getReason()
        );
    }
}
