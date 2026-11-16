package hu.szakdolgozat.borbely.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

/**
 * A HTTP-szintű biztonsági szabályok egy helyen: ki melyik végpontot érheti el,
 * állapotmentes JWT-hitelesítés, CORS, valamint a 401-es és 403-as hibák egységes kezelése.
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final CorsProperties corsProperties;
    private final HandlerExceptionResolver resolver;

    public SecurityConfig(JwtAuthenticationConverter jwtAuthenticationConverter,
                          CorsProperties corsProperties,
                          @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.corsProperties = corsProperties;
        this.resolver = resolver;
    }

    /**
     * A szabályok sorrendje számít: az első illeszkedő dönt.
     * CSRF-védelem azért nem kell, mert a kliens nem sütivel, hanem Bearer tokennel azonosít,
     * így egy idegen oldal nem tud a felhasználó nevében hitelesített kérést küldeni.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // A hibaoldal szabad, különben egy 400-as vagy 404-es hiba is 401-nek látszana.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        // A beosztás nem nyilvános: borbély vagy tulajdonos érheti el; hogy kiét, azt a ScheduleAccess dönti el.
                        .requestMatchers("/api/barbers/*/availability/**", "/api/barbers/*/time-off/**")
                        .hasAnyRole("BARBER", "OWNER")
                        // A foglalási tölcsér belépés nélkül is böngészhető.
                        .requestMatchers(HttpMethod.GET, "/api/services/**", "/api/barbers/**").permitAll()
                        .requestMatchers("/api/services/**", "/api/barbers/**").hasRole("OWNER")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        // Hibás, hamisított vagy lejárt token.
                        .authenticationEntryPoint(this::delegate)
                        .accessDeniedHandler(this::delegate))
                .exceptionHandling(exceptions -> exceptions
                        // Hiányzó token.
                        .authenticationEntryPoint(this::delegate)
                        // Bejelentkezett, de a szerepköre nem engedi.
                        .accessDeniedHandler(this::delegate));
        return http.build();
    }

    /**
     * Csak az Angular-alkalmazás címéről (app.cors.allowed-origins) enged böngészőből érkező hívást.
     * A Location fejlécet is láthatóvá teszi, mert a létrehozó végpontok abban adják vissza az új elem címét.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(corsProperties.allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setExposedHeaders(List.of("Location"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    /**
     * A biztonsági hibákat a GlobalExceptionHandlerhez továbbítja, így azok is ugyanolyan
     * ProblemDetail-válaszként jönnek, mint az alkalmazás többi hibája.
     */
    private void delegate(HttpServletRequest request, HttpServletResponse response, Exception exception) {
        resolver.resolveException(request, response, null, exception);
    }
}
