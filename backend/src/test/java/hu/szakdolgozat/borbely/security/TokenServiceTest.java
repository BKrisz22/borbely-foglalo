package hu.szakdolgozat.borbely.security;

import hu.szakdolgozat.borbely.user.AppUser;
import hu.szakdolgozat.borbely.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A tokenkiadás és -ellenőrzés egységtesztjei, Spring-környezet nélkül.
 */
class TokenServiceTest {

    private static final String SECRET = base64("tesztkulcs-legalabb-32-bajt-hosszu!!");
    private static final String OTHER_SECRET = base64("egy-masik-kulcs-ami-szinten-eleg-hosszu");
    private static final String ISSUER = "borbely-teszt";

    private final JwtConfig config = new JwtConfig();

    @Test
    void issuedTokenCarriesUserIdRoleIssuerAndExpiry() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        JwtProperties properties = properties(SECRET);
        SecretKey key = config.jwtSecretKey(properties);
        TokenService tokens = new TokenService(config.jwtEncoder(key), properties, Clock.fixed(now, ZoneOffset.UTC));

        IssuedToken issued = tokens.issue(user(42L, Role.OWNER));
        Jwt jwt = config.jwtDecoder(key, properties).decode(issued.token());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString(TokenService.ROLE_CLAIM)).isEqualTo("OWNER");
        assertThat(jwt.getClaimAsString(JwtClaimNames.ISS)).isEqualTo(ISSUER);
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofHours(1)));
        assertThat(issued.expiresAt()).isEqualTo(jwt.getExpiresAt());
    }

    @Test
    void expiredTokenIsRejected() {
        Instant twoHoursAgo = Instant.now().minus(Duration.ofHours(2));
        JwtProperties properties = properties(SECRET);
        SecretKey key = config.jwtSecretKey(properties);
        TokenService tokens = new TokenService(
                config.jwtEncoder(key), properties, Clock.fixed(twoHoursAgo, ZoneOffset.UTC));

        String token = tokens.issue(user(42L, Role.CUSTOMER)).token();

        assertThatThrownBy(() -> config.jwtDecoder(key, properties).decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtProperties properties = properties(SECRET);
        SecretKey key = config.jwtSecretKey(properties);
        String token = new TokenService(config.jwtEncoder(key), properties, Clock.systemUTC())
                .issue(user(42L, Role.CUSTOMER)).token();

        JwtProperties otherProperties = properties(OTHER_SECRET);
        JwtDecoder otherDecoder = config.jwtDecoder(config.jwtSecretKey(otherProperties), otherProperties);

        assertThatThrownBy(() -> otherDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void secretThatIsNotBase64IsRejected() {
        assertThatThrownBy(() -> config.jwtSecretKey(properties("change-me")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    @Test
    void tooShortSecretIsRejected() {
        String sixteenBytes = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> config.jwtSecretKey(properties(sixteenBytes)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256");
    }

    private static JwtProperties properties(String secret) {
        return new JwtProperties(secret, ISSUER, Duration.ofHours(1));
    }

    private static AppUser user(long id, Role role) {
        AppUser user = new AppUser("teszt@example.com", "nem-hasznalt-hash", "Teszt Elek", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }
}
