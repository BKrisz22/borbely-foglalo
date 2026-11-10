package hu.szakdolgozat.borbely.barber.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record BarberCreateRequest(
        @NotBlank(message = "Az e-mail-cím megadása kötelező.")
        @Email(message = "Érvénytelen e-mail-cím.")
        @Size(max = 254, message = "Az e-mail-cím legfeljebb 254 karakter lehet.")
        String email,

        @NotBlank(message = "A név megadása kötelező.")
        @Size(max = 100, message = "A név legfeljebb 100 karakter lehet.")
        String fullName,

        @Size(max = 500, message = "A bemutatkozás legfeljebb 500 karakter lehet.")
        String bio,

        @Min(value = 0, message = "A foglalási határidő nem lehet negatív.")
        @Max(value = 1440, message = "A foglalási határidő legfeljebb 1440 perc.")
        Integer minLeadMinutes,

        Set<@NotNull(message = "A szolgáltatás-azonosító nem lehet üres.") Long> serviceIds
) {
}
