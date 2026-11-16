package hu.szakdolgozat.borbely.slot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Melyik keresőalgoritmus fusson. Éles működésben a csúszóablakos (O(n)); a naiv változat
 * csak a tesztekben és az összehasonlító mérésben szerepel.
 */
@Configuration
public class SlotConfig {

    @Bean
    public SlotFinder slotFinder() {
        return new SlidingWindowSlotFinder();
    }
}
