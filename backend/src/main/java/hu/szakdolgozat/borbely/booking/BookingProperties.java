package hu.szakdolgozat.borbely.booking;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * A foglalás beállításai ({@code app.booking}). A {@code cancelDeadline} azt adja meg, legkésőbb
 * mennyivel a kezdés előtt mondhatja le a vendég a foglalását az appban (alapból 24 óra);
 * ezután már csak telefonon, a szalonon keresztül.
 */
@Validated
@ConfigurationProperties(prefix = "app.booking")
public record BookingProperties(
        @NotNull Duration cancelDeadline
) {
}
