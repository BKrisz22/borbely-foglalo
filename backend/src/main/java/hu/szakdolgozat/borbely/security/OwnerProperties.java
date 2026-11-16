package hu.szakdolgozat.borbely.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Az induláskor létrehozandó tulajdonosi fiók adatai (app.owner.*).
 * Mindkét érték elhagyható; ilyenkor nem jön létre tulajdonosi fiók.
 */
@ConfigurationProperties("app.owner")
public record OwnerProperties(String email, String password) {

    /** Igaz, ha az e-mail-cím és a jelszó is meg van adva. */
    public boolean isConfigured() {
        return email != null && !email.isBlank()
                && password != null && !password.isBlank();
    }

    @Override
    public String toString() {
        return "OwnerProperties[email=" + email + ", password=***]";
    }
}