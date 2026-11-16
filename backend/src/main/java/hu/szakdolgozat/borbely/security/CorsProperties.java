package hu.szakdolgozat.borbely.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * A böngészőből hívható eredetek listája (app.cors.*), például az Angular fejlesztői szerver.
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}