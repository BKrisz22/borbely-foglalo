package hu.szakdolgozat.borbely.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * A JWT-tokenek aláírásához és ellenőrzéséhez szükséges beanek (HS256, közös titkos kulccsal).
 */
@Configuration
public class JwtConfig {

    /** A HS256 aláíráshoz legalább 256 bites, azaz 32 bájtos kulcs kell. */
    static final int MIN_KEY_BYTES = 32;

    /**
     * A Base64 kódolású titokból HMAC-kulcsot készít. Érvénytelen vagy túl rövid titok esetén
     * az alkalmazás már induláskor leáll, nem csak az első bejelentkezéskor.
     */
    @Bean
    public SecretKey jwtSecretKey(JwtProperties properties) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(properties.secret());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Az app.jwt.secret (JWT_SECRET) nem érvényes Base64 szöveg.", e);
        }
        if (bytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("Az app.jwt.secret (JWT_SECRET) legalább " + MIN_KEY_BYTES
                    + " bájtos (256 bites) kulcs legyen, a megadott csak " + bytes.length + " bájtos.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    /**
     * Ellenőrzi az aláírást, a lejáratot és a kibocsátót. Bármelyik hibája esetén a kérés 401-et kap.
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    /**
     * A token "role" claimjéből ROLE_ előtagú jogosultságot készít, így a hasRole("OWNER")
     * szabályok közvetlenül a tokenből dolgoznak, adatbázis-lekérdezés nélkül.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(TokenService.ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
