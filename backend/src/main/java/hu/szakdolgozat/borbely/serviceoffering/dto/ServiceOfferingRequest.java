package hu.szakdolgozat.borbely.serviceoffering.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ServiceOfferingRequest(
        @NotBlank(message = "A név megadása kötelező.")
        @Size(max = 100, message = "A név legfeljebb 100 karakter lehet.")
        String name,

        @Size(max = 500, message = "A leírás legfeljebb 500 karakter lehet.")
        String description,

        @NotNull(message = "Az időtartam megadása kötelező.")
        @Min(value = 1, message = "Az időtartam legalább 1 perc.")
        @Max(value = 480, message = "Az időtartam legfeljebb 480 perc.")
        Integer durationMinutes,

        @NotNull(message = "Az ár megadása kötelező.")
        @PositiveOrZero(message = "Az ár nem lehet negatív.")
        Integer priceHuf,

        Boolean active
) {
}
