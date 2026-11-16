package hu.szakdolgozat.borbely.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Az alkalmazás órája. Beanként kerül be, hogy a tesztekben rögzített időre cserélhető legyen.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
