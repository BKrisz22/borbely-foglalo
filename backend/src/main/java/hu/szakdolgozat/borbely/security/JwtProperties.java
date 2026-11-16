package hu.szakdolgozat.borbely.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * A JWT-tokenek kiadásához és ellenőrzéséhez szükséges beállítások (app.jwt.*).
 * Hiányzó vagy üres érték esetén az alkalmazás el sem indul.
 */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotBlank String issuer,
        @NotNull Duration ttl
) {

    @Override
    public String toString() {
        return "JwtProperties[secret=***, issuer=" + issuer + ", ttl=" + ttl + "]";
    }
}