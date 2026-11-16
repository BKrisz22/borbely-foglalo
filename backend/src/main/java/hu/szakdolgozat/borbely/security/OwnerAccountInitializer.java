package hu.szakdolgozat.borbely.security;

import hu.szakdolgozat.borbely.common.StrongPasswordValidator;
import hu.szakdolgozat.borbely.user.AppUser;
import hu.szakdolgozat.borbely.user.AppUserRepository;
import hu.szakdolgozat.borbely.user.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Induláskor létrehozza a tulajdonosi fiókot az {@code OWNER_EMAIL} és {@code OWNER_PASSWORD}
 * beállításból, ha még nincs tulajdonos. Így a tulajdonos jelszava sem a repóba, sem a Liquibase-be
 * nem kerül. Ha a beállítás hiányzik, csak figyelmeztet, így a tesztek tulajdonos nélkül is futnak.
 */
@Component
public class OwnerAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OwnerAccountInitializer.class);

    private final OwnerProperties properties;
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public OwnerAccountInitializer(OwnerProperties properties, AppUserRepository users,
                                   PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRole(Role.OWNER)) {
            return;
        }
        if (!properties.isConfigured()) {
            log.warn("Nincs tulajdonosi fiók, és az OWNER_EMAIL / OWNER_PASSWORD sincs megadva, "
                    + "ezért tulajdonosként nem lehet belépni.");
            return;
        }
        // Hibás beállításnál az alkalmazás el sem indul, hogy a hiba azonnal kiderüljön.
        String problem = StrongPasswordValidator.problem(properties.password());
        if (problem != null) {
            throw new IllegalStateException("Az OWNER_PASSWORD nem megfelelő: " + problem);
        }
        String email = AppUser.normalizeEmail(properties.email());
        if (users.existsByEmail(email)) {
            throw new IllegalStateException(
                    "Az OWNER_EMAIL címmel (" + email + ") már létezik egy nem tulajdonosi fiók.");
        }
        users.save(new AppUser(email, passwordEncoder.encode(properties.password()), "Tulajdonos", Role.OWNER));
        log.info("Tulajdonosi fiók létrehozva: {}", email);
    }
}
