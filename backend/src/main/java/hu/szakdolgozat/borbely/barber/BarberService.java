package hu.szakdolgozat.borbely.barber;

import hu.szakdolgozat.borbely.barber.dto.BarberCreateRequest;
import hu.szakdolgozat.borbely.barber.dto.BarberResponse;
import hu.szakdolgozat.borbely.barber.dto.BarberServicesRequest;
import hu.szakdolgozat.borbely.barber.dto.BarberUpdateRequest;
import hu.szakdolgozat.borbely.common.ConflictException;
import hu.szakdolgozat.borbely.common.NotFoundException;
import hu.szakdolgozat.borbely.serviceoffering.ServiceOffering;
import hu.szakdolgozat.borbely.serviceoffering.ServiceOfferingRepository;
import hu.szakdolgozat.borbely.user.AppUser;
import hu.szakdolgozat.borbely.user.AppUserRepository;
import hu.szakdolgozat.borbely.user.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * A borbélyok üzleti logikája. Új borbély létrehozásakor a hozzá tartozó
 * {@code BARBER} szerepkörű felhasználói fiók is létrejön.
 */
@Service
public class BarberService {

    private static final Locale HUNGARIAN = Locale.of("hu", "HU");
    /**
     * Jelszó nélküli fiók jelölése: nem érvényes BCrypt-hash,
     * ezért ezzel a fiókkal nem lehet belépni.
     */
    private static final String NO_PASSWORD = "!";

    private final BarberRepository barbers;
    private final AppUserRepository users;
    private final ServiceOfferingRepository serviceOfferings;

    public BarberService(BarberRepository barbers, AppUserRepository users,
                         ServiceOfferingRepository serviceOfferings) {
        this.barbers = barbers;
        this.users = users;
        this.serviceOfferings = serviceOfferings;
    }

    /**
     * A borbélyokat név szerint, a magyar ábécé szabályai szerint rendezi ({@link Collator}),
     * így az ékezetes betűk is a helyükre kerülnek.
     */
    @Transactional(readOnly = true)
    public List<BarberResponse> list(Long serviceId, boolean includeInactive) {
        List<Barber> result = includeInactive ? barbers.findAll() : barbers.findAllByActiveTrue();
        Collator collator = Collator.getInstance(HUNGARIAN);
        return result.stream()
                .filter(barber -> serviceId == null || offers(barber, serviceId))
                .sorted(Comparator.comparing((Barber barber) -> barber.getUser().getFullName(), collator))
                .map(BarberResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BarberResponse get(Long id) {
        return BarberResponse.from(find(id));
    }

    @Transactional
    public BarberResponse create(BarberCreateRequest request) {
        String email = AppUser.normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("Ezzel az e-mail-címmel már van fiók: " + email);
        }
        List<ServiceOffering> services = request.serviceIds() == null
                ? List.of()
                : loadServices(request.serviceIds());

        AppUser user = users.save(new AppUser(email, NO_PASSWORD, request.fullName().trim(), Role.BARBER));

        Barber barber = new Barber(user, blankToNull(request.bio()));
        if (request.minLeadMinutes() != null) {
            barber.setMinLeadMinutes(request.minLeadMinutes());
        }
        services.forEach(barber::addService);
        return BarberResponse.from(barbers.save(barber));
    }

    @Transactional
    public BarberResponse update(Long id, BarberUpdateRequest request) {
        Barber barber = find(id);
        barber.getUser().setFullName(request.fullName().trim());
        barber.setBio(blankToNull(request.bio()));
        barber.setMinLeadMinutes(request.minLeadMinutes());
        if (request.active() != null) {
            barber.setActive(request.active());
        }
        return BarberResponse.from(barber);
    }

    @Transactional
    public BarberResponse replaceServices(Long id, BarberServicesRequest request) {
        Barber barber = find(id);
        List<ServiceOffering> newServices = loadServices(request.serviceIds());
        List.copyOf(barber.getServices()).forEach(barber::removeService);
        newServices.forEach(barber::addService);
        return BarberResponse.from(barber);
    }

    @Transactional
    public void deactivate(Long id) {
        find(id).setActive(false);
    }

    private Barber find(Long id) {
        return barbers.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Nincs ilyen borbély: " + id));
    }

    private List<ServiceOffering> loadServices(Set<Long> ids) {
        List<ServiceOffering> found = serviceOfferings.findAllById(ids);
        if (found.size() != ids.size()) {
            Set<Long> missing = new TreeSet<>(ids);
            found.forEach(service -> missing.remove(service.getId()));
            throw new NotFoundException("Nincs ilyen szolgáltatás: " + missing);
        }
        return found;
    }

    private static boolean offers(Barber barber, Long serviceId) {
        return barber.getServices().stream().anyMatch(service -> service.getId().equals(serviceId));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
