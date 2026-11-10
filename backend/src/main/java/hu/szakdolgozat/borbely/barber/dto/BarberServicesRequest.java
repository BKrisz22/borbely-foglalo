package hu.szakdolgozat.borbely.barber.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record BarberServicesRequest(
        @NotNull(message = "A szolgáltatások listája kötelező (lehet üres is).")
        Set<@NotNull(message = "A szolgáltatás-azonosító nem lehet üres.") Long> serviceIds
) {
}
