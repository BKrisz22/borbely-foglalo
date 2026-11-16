package hu.szakdolgozat.borbely.schedule;

import hu.szakdolgozat.borbely.barber.Barber;
import hu.szakdolgozat.borbely.barber.BarberRepository;
import hu.szakdolgozat.borbely.common.NotFoundException;
import hu.szakdolgozat.borbely.security.Authentications;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Eldönti, ki kezelheti egy borbély beosztását: a borbély a sajátját, a tulajdonos bárkiét.
 * Azt, hogy a kérés borbélytól vagy tulajdonostól jön, már a {@code SecurityConfig} ellenőrizte;
 * itt az számít, hogy kié a beosztás. A szolgáltatások tranzakcióján belül kell hívni.
 */
@Component
public class ScheduleAccess {

    private final BarberRepository barbers;

    public ScheduleAccess(BarberRepository barbers) {
        this.barbers = barbers;
    }

    /** Visszaadja a borbélyt, ha a bejelentkezett felhasználó kezelheti a beosztását; különben 403. */
    public Barber requireManageable(Long barberId, Authentication authentication) {
        Barber barber = barbers.findById(barberId)
                .orElseThrow(() -> new NotFoundException("Nincs ilyen borbély: " + barberId));
        if (Authentications.isOwner(authentication)) {
            return barber;
        }
        Long userId = Authentications.userId(authentication);
        if (userId == null || !userId.equals(barber.getUser().getId())) {
            throw new AccessDeniedException("Csak a saját beosztásodat kezelheted.");
        }
        return barber;
    }
}
