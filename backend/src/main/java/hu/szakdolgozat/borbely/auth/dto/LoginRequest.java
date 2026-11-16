package hu.szakdolgozat.borbely.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Belépés e-mail-címmel és jelszóval ({@code POST /api/auth/login}).
 * A jelszószabályt itt szándékosan nem ellenőrzi: a régi jelszavak is működjenek.
 */
public record LoginRequest(
        @NotBlank(message = "Az e-mail-cím megadása kötelező.")
        String email,

        @NotBlank(message = "A jelszó megadása kötelező.")
        String password
) {
}
