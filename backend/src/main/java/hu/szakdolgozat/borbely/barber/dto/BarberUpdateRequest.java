package hu.szakdolgozat.borbely.barber.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Borbély adatainak módosítása ({@code PUT /api/barbers/{id}}).
 * Az {@code active} mező elhagyható; ha nincs megadva, nem változik.
 */
public record BarberUpdateRequest(
        @NotBlank(message = "A név megadása kötelező.")
        @Size(max = 100, message = "A név legfeljebb 100 karakter lehet.")
        String fullName,

        @Size(max = 500, message = "A bemutatkozás legfeljebb 500 karakter lehet.")
        String bio,

        @NotNull(message = "A foglalási határidő megadása kötelező.")
        @Min(value = 0, message = "A foglalási határidő nem lehet negatív.")
        @Max(value = 1440, message = "A foglalási határidő legfeljebb 1440 perc.")
        Integer minLeadMinutes,

        Boolean active
) {
}
