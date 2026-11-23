package hu.szakdolgozat.borbely.barber.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A borbély saját profiljának módosítása ({@code PUT /api/barbers/{id}/profile}): a bemutatkozás
 * és az, hogy legkésőbb hány perccel a kezdés előtt foglalhatnak nála a vendégek.
 */
public record BarberProfileRequest(
        @Size(max = 500, message = "A bemutatkozás legfeljebb 500 karakter lehet.")
        String bio,

        @NotNull(message = "A foglalási határidő megadása kötelező.")
        @Min(value = 0, message = "A foglalási határidő nem lehet negatív.")
        @Max(value = 1440, message = "A foglalási határidő legfeljebb 1440 perc.")
        Integer minLeadMinutes
) {
}
