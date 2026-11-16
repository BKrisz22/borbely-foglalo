package hu.szakdolgozat.borbely.schedule;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

/**
 * A beosztás beállításai ({@code app.schedule}). A borbélyok a munkaidőt helyi (budapesti) időben
 * adják meg, a foglalások és a távollétek viszont UTC-ben tárolódnak; a kettő között a {@code zone} vált.
 * A {@code bookingHorizonDays} azt adja meg, legfeljebb hány nappal előre lehet foglalni.
 */
@Validated
@ConfigurationProperties(prefix = "app.schedule")
public record ScheduleProperties(
        @NotNull ZoneId zone,
        @Positive int bookingHorizonDays
) {
}
