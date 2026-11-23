package hu.szakdolgozat.borbely.barber;

import hu.szakdolgozat.borbely.barber.dto.BarberCreateRequest;
import hu.szakdolgozat.borbely.barber.dto.BarberProfileRequest;
import hu.szakdolgozat.borbely.barber.dto.BarberPublicResponse;
import hu.szakdolgozat.borbely.barber.dto.BarberResponse;
import hu.szakdolgozat.borbely.barber.dto.BarberServicesRequest;
import hu.szakdolgozat.borbely.barber.dto.BarberUpdateRequest;
import hu.szakdolgozat.borbely.barber.dto.BarberView;
import hu.szakdolgozat.borbely.common.ConflictException;
import hu.szakdolgozat.borbely.common.NotFoundException;
import hu.szakdolgozat.borbely.security.Authentications;
import hu.szakdolgozat.borbely.serviceoffering.ServiceOffering;
import hu.szakdolgozat.borbely.serviceoffering.ServiceOfferingRepository;
import hu.szakdolgozat.borbely.user.AppUser;
import hu.szakdolgozat.borbely.user.AppUserRepository;
import hu.szakdolgozat.borbely.user.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    private final BarberRepository barbers;
    private final AppUserRepository users;
    private final ServiceOfferingRepository serviceOfferings;
    private final PasswordEncoder passwordEncoder;

    public BarberService(BarberRepository barbers, AppUserRepository users,
                         ServiceOfferingRepository serviceOfferings, PasswordEncoder passwordEncoder) {
        this.barbers = barbers;
        this.users = users;
        this.serviceOfferings = serviceOfferings;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * A borbélyokat név szerint, a magyar ábécé szabályai szerint rendezi ({@link Collator}),
     * így az ékezetes betűk is a helyükre kerülnek.
     */
    @Transactional(readOnly = true)
    public List<BarberResponse> list(Long serviceId, boolean includeInactive) {
        List<Barber> result = includeInactive ? barbers.findAll() : barbers.findAllByActiveTrue();
        return filterAndSort(result, serviceId).stream().map(BarberResponse::from).toList();
    }

    /** Nyilvános lista: csak az aktív borbélyok, e-mail-cím nélkül, magyar névsorban. */
    @Transactional(readOnly = true)
    public List<BarberPublicResponse> listPublic(Long serviceId) {
        return filterAndSort(barbers.findAllByActiveTrue(), serviceId).stream()
                .map(BarberPublicResponse::from)
                .toList();
    }

    /**
     * Egy borbély a kérő szerint: a tulajdonos és maga a borbély a teljes adatokat kapja, más
     * csak egy aktív borbély nyilvános adatait; kikapcsolt borbélynál 404, mintha nem is létezne.
     */
    @Transactional(readOnly = true)
    public BarberView getFor(Long id, Authentication authentication) {
        Barber barber = find(id);
        if (Authentications.isOwner(authentication) || isOwnProfile(barber, authentication)) {
            return BarberResponse.from(barber);
        }
        if (!barber.isActive()) {
            throw new NotFoundException("Nincs ilyen borbély: " + id);
        }
        return BarberPublicResponse.from(barber);
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

        // A kezdőjelszót a tulajdonos adja meg; a borbélynak az első belépés után le kell cserélnie.
        AppUser newUser = new AppUser(email, passwordEncoder.encode(request.initialPassword()),
                request.fullName().trim(), Role.BARBER);
        newUser.setMustChangePassword(true);
        AppUser user = users.save(newUser);

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

    /**
     * A borbély saját profilja: bemutatkozás és foglalási határidő. A borbély a sajátját,
     * a tulajdonos bárkiét módosíthatja; a nevet, az aktív állapotot és a szolgáltatásokat csak a tulajdonos.
     */
    @Transactional
    public BarberResponse updateProfile(Long id, BarberProfileRequest request, Authentication authentication) {
        Barber barber = find(id);
        if (!Authentications.isOwner(authentication) && !isOwnProfile(barber, authentication)) {
            throw new AccessDeniedException("Csak a saját profilodat módosíthatod.");
        }
        barber.setBio(blankToNull(request.bio()));
        barber.setMinLeadMinutes(request.minLeadMinutes());
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

    /** A bejelentkezett felhasználó maga ez a borbély-e (névtelen kérésnél nem). */
    private static boolean isOwnProfile(Barber barber, Authentication authentication) {
        return barber.getUser().getId().equals(Authentications.userId(authentication));
    }

    /** Szolgáltatás szerinti szűrés, majd névsor a magyar ábécé szerint ({@link Collator}). */
    private static List<Barber> filterAndSort(List<Barber> candidates, Long serviceId) {
        Collator collator = Collator.getInstance(HUNGARIAN);
        return candidates.stream()
                .filter(barber -> serviceId == null || offers(barber, serviceId))
                .sorted(Comparator.comparing((Barber barber) -> barber.getUser().getFullName(), collator))
                .toList();
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
