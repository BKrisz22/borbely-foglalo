package hu.szakdolgozat.borbely.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Jelszókezelés és az e-mail-cím + jelszó alapú hitelesítés (a bejelentkezési végponthoz).
 */
@Configuration
public class AuthenticationConfig {

    /**
     * BCrypt jelszóhash. Szándékosan nem DelegatingPasswordEncoder: az a jelszó nélküli fiókok
     * "!" hash-ére kivételt dobna, a BCrypt viszont egyszerűen nem egyezőnek tekinti.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Az e-mail-cím és a jelszó ellenőrzése az adatbázisban tárolt hash alapján.
     * A DaoAuthenticationProvider ismeretlen felhasználónál is elvégez egy jelszó-összehasonlítást,
     * így a válaszidőből sem derül ki, hogy létezik-e a fiók.
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
