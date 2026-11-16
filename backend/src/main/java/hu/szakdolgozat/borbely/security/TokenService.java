package hu.szakdolgozat.borbely.security;

import hu.szakdolgozat.borbely.user.AppUser;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Sikeres bejelentkezés után HS256-tal aláírt JWT hozzáférési tokent ad ki.
 * A token alanya (sub) a felhasználó azonosítója, a szerepköre a "role" claimben szerepel.
 */
@Service
public class TokenService {

    /** A szerepkört tartalmazó claim neve; a JwtAuthenticationConverter is ezt olvassa. */
    public static final String ROLE_CLAIM = "role";

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedToken issue(AppUser user) {
        Objects.requireNonNull(user.getId(), "Csak elmentett felhasználónak adható ki token.");

        // A JWT másodperc pontosságú, ezért a válaszban visszaadott lejárat is az legyen.
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(properties.ttl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(ROLE_CLAIM, user.getRole().name())
                .build();

        // Az algoritmust kötelező megadni: enélkül a kódoló RSA-kulcsot keresne.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
