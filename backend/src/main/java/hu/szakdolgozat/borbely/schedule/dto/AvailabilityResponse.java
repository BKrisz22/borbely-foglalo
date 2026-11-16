package hu.szakdolgozat.borbely.schedule.dto;

import hu.szakdolgozat.borbely.schedule.Availability;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Egy munkaidő-sáv, ahogy a borbély és a tulajdonos látja.
 */
public record AvailabilityResponse(
        Long id,
        Long barberId,
        LocalDate workDate,
        LocalTime startTime,
        LocalTime endTime
) {

    public static AvailabilityResponse from(Availability availability) {
        return new AvailabilityResponse(
                availability.getId(),
                availability.getBarber().getId(),
                availability.getWorkDate(),
                availability.getStartTime(),
                availability.getEndTime()
        );
    }
}
