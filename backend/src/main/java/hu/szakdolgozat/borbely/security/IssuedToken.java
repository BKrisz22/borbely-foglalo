package hu.szakdolgozat.borbely.security;

import java.time.Instant;

/**
 * Egy kiadott hozzáférési token és a lejárati ideje.
 */
public record IssuedToken(String token, Instant expiresAt) {
}
