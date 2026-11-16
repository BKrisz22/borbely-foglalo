package hu.szakdolgozat.borbely.barber.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * A borbély által nyújtott szolgáltatások teljes cseréje
 * ({@code PUT /api/barbers/{id}/services}). Üres lista esetén minden szolgáltatás lekerül a borbélyról.
 */
public record BarberServicesRequest(
        @NotNull(message = "A szolgáltatások listája kötelező (lehet üres is).")
        Set<@NotNull(message = "A szolgáltatás-azonosító nem lehet üres.") Long> serviceIds
) {
}
