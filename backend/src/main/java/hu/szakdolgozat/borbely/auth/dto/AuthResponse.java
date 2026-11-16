package hu.szakdolgozat.borbely.auth.dto;

import java.time.Instant;

/**
 * Sikeres regisztráció vagy belépés válasza: a hozzáférési token, a lejárata és a felhasználó adatai.
 * A kliens a tokent az {@code Authorization: Bearer ...} fejlécben küldi vissza.
 */
public record AuthResponse(
        String token,
        Instant expiresAt,
        UserResponse user
) {
}
