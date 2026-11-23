package hu.szakdolgozat.borbely.booking;

import hu.szakdolgozat.borbely.barber.Barber;
import hu.szakdolgozat.borbely.barber.BarberRepository;
import hu.szakdolgozat.borbely.booking.dto.BookingCreateRequest;
import hu.szakdolgozat.borbely.booking.dto.BookingResponse;
import hu.szakdolgozat.borbely.common.BadRequestException;
import hu.szakdolgozat.borbely.common.ConflictException;
import hu.szakdolgozat.borbely.common.NotFoundException;
import hu.szakdolgozat.borbely.schedule.ScheduleTime;
import hu.szakdolgozat.borbely.security.Authentications;
import hu.szakdolgozat.borbely.serviceoffering.ServiceOffering;
import hu.szakdolgozat.borbely.slot.SlotService;
import hu.szakdolgozat.borbely.user.AppUser;
import hu.szakdolgozat.borbely.user.AppUserRepository;
import hu.szakdolgozat.borbely.user.Role;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * A foglalások kezelése: létrehozás, listázás, lemondás és lezárás.
 *
 * <p>Ki mit tehet:
 * <ul>
 *   <li>a vendég a saját nevére foglal, a saját foglalásait látja, és a kezdés előtt legalább
 *       {@code app.booking.cancel-deadline} idővel (alapból 24 óra) lemondhatja őket;</li>
 *   <li>a borbély a saját naptárába egy regisztrált vendég nevében is foglalhat (például telefonon),
 *       a kezdésig lemondhat, a kezdés után lezárhat (megjelent / nem jelent meg);</li>
 *   <li>a tulajdonos mindezt bármelyik borbélynál megteheti.</li>
 * </ul>
 *
 * <p>Párhuzamos foglalások ellen két védelem van. Ha két kérés ugyanarra a borbélyra és időre
 * érkezik, az adatbázis {@code booking_no_overlap} kizárási megkötése a másodikat elutasítja (409).
 * Egy vendégnek különböző borbélyoknál sem lehet két átfedő foglalása: ezt az alkalmazás ellenőrzi,
 * és hogy két egyidejű kérés ne csússzon át az ellenőrzésen, a vendég sorát a tranzakció végéig zárolja.
 */
@Service
public class BookingService {

    private final BookingRepository bookings;
    private final AppUserRepository users;
    private final BarberRepository barbers;
    private final SlotService slots;
    private final ScheduleTime time;
    private final BookingProperties properties;

    public BookingService(BookingRepository bookings, AppUserRepository users, BarberRepository barbers,
                          SlotService slots, ScheduleTime time, BookingProperties properties) {
        this.bookings = bookings;
        this.users = users;
        this.barbers = barbers;
        this.slots = slots;
        this.time = time;
        this.properties = properties;
    }

    @Transactional
    public BookingResponse create(BookingCreateRequest request, Authentication authentication) {
        boolean byCustomer = Authentications.hasRole(authentication, Role.CUSTOMER);
        Long actorId = Authentications.userId(authentication);
        Long customerId;
        if (byCustomer) {
            customerId = requireActor(actorId);
            if (request.customerId() != null && !request.customerId().equals(customerId)) {
                throw new AccessDeniedException("Vendégként csak a saját nevedre foglalhatsz.");
            }
        } else {
            customerId = request.customerId();
            if (customerId == null) {
                throw new BadRequestException("Add meg, melyik vendég nevére szól a foglalás (customerId).");
            }
        }

        Barber barber = slots.requireActiveBarber(request.barberId());
        if (!byCustomer && !Authentications.isOwner(authentication)
                && !barber.getUser().getId().equals(requireActor(actorId))) {
            throw new AccessDeniedException("Borbélyként csak a saját naptáradba vehetsz fel foglalást.");
        }
        ServiceOffering service = SlotService.requireOffered(barber, request.serviceId());

        // A vendég sorának zárolása: ugyanannak a vendégnek két egyidejű foglalása innentől egymás után fut.
        AppUser customer = users.findWithLockById(customerId)
                .filter(user -> user.getRole() == Role.CUSTOMER && user.isEnabled())
                .orElseThrow(() -> new BadRequestException(
                        "Foglalni csak regisztrált, aktív vendég nevére lehet: " + customerId));

        Instant startAt = request.startAt().toInstant();
        if (!slots.isBookableStart(barber, service, startAt, byCustomer)) {
            throw new ConflictException("Ez az időpont nem foglalható: közben lefoglalták, vagy kívül esik "
                    + "a foglalható időn. Kérjük, válassz újat a szabad időpontok közül.");
        }
        Booking booking = new Booking(barber, customer, service, startAt);
        if (bookings.existsActiveForCustomerOverlapping(customer.getId(), booking.getStartAt(), booking.getEndAt())) {
            throw new ConflictException(byCustomer
                    ? "Erre az időre már van foglalásod."
                    : "A vendégnek erre az időre már van foglalása.");
        }
        try {
            bookings.saveAndFlush(booking);
        } catch (DataIntegrityViolationException e) {
            // Két vendég egyszerre ugyanarra az időpontra: a booking_no_overlap megkötés a másodikat elutasítja.
            throw new ConflictException("Ezt az időpontot közben valaki más lefoglalta. Kérjük, válassz újat.");
        }
        return BookingResponse.from(booking);
    }

    /**
     * A két nap (helyi idő szerint, mindkettőt beleértve) közé eső foglalások, minden állapotban.
     * A vendég a sajátjait, a borbély a saját naptárát, a tulajdonos mindet látja; a {@code barberId}
     * szűrő elhagyható.
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> list(LocalDate from, LocalDate to, Long barberId, Authentication authentication) {
        ScheduleTime.requireValidRange(from, to);
        Instant start = time.startOfDay(from);
        Instant end = time.startOfDay(to.plusDays(1));
        List<Booking> result;
        if (Authentications.isOwner(authentication)) {
            result = barberId == null
                    ? bookings.findAllBetween(start, end)
                    : bookings.findForBarber(barberId, start, end);
        } else if (Authentications.hasRole(authentication, Role.BARBER)) {
            Barber own = barbers.findByUserId(requireActor(Authentications.userId(authentication)))
                    .orElseThrow(() -> new NotFoundException("Ehhez a fiókhoz nem tartozik borbélyprofil."));
            if (barberId != null && !barberId.equals(own.getId())) {
                throw new AccessDeniedException("Csak a saját naptáradat láthatod.");
            }
            result = bookings.findForBarber(own.getId(), start, end);
        } else {
            result = bookings.findForCustomer(requireActor(Authentications.userId(authentication)), start, end)
                    .stream()
                    .filter(booking -> barberId == null || booking.getBarber().getId().equals(barberId))
                    .toList();
        }
        return result.stream().map(BookingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse get(Long id, Authentication authentication) {
        return BookingResponse.from(findVisible(id, authentication));
    }

    /**
     * Lemondás. A vendég a kezdés előtt legalább {@code cancelDeadline} idővel mondhatja le,
     * a borbély és a tulajdonos a kezdésig. A lemondott időpont azonnal újra foglalható.
     */
    @Transactional
    public BookingResponse cancel(Long id, Authentication authentication) {
        Booking booking = findVisible(id, authentication);
        requireBooked(booking);
        Instant now = time.now();
        if (isStaff(booking, authentication)) {
            if (!now.isBefore(booking.getStartAt())) {
                throw new ConflictException("Elkezdődött foglalás már nem mondható le.");
            }
        } else if (now.isAfter(booking.getStartAt().minus(properties.cancelDeadline()))) {
            throw new ConflictException("A kezdés előtt " + properties.cancelDeadline().toHours()
                    + " órán belül az appban már nem mondható le a foglalás; kérjük, hívd a szalont.");
        }
        booking.cancel();
        return BookingResponse.from(booking);
    }

    /** Lezárás: a vendég megjelent. Csak a borbély vagy a tulajdonos, a kezdés után. */
    @Transactional
    public BookingResponse complete(Long id, Authentication authentication) {
        return close(id, authentication, Booking::complete);
    }

    /** Lezárás: a vendég nem jelent meg. Csak a borbély vagy a tulajdonos, a kezdés után. */
    @Transactional
    public BookingResponse markNoShow(Long id, Authentication authentication) {
        return close(id, authentication, Booking::markNoShow);
    }

    private BookingResponse close(Long id, Authentication authentication, Consumer<Booking> action) {
        Booking booking = findVisible(id, authentication);
        if (!isStaff(booking, authentication)) {
            throw new AccessDeniedException("A foglalást csak a borbély vagy a tulajdonos zárhatja le.");
        }
        requireBooked(booking);
        if (time.now().isBefore(booking.getStartAt())) {
            throw new ConflictException("A foglalás csak a kezdése után zárható le.");
        }
        action.accept(booking);
        return BookingResponse.from(booking);
    }

    /**
     * A foglalás, ha a bejelentkezett felhasználó láthatja: a vendége, a borbélya vagy a tulajdonos.
     * Másnak 404, hogy a válaszból ne derüljön ki, létezik-e.
     */
    private Booking findVisible(Long id, Authentication authentication) {
        Booking booking = bookings.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Nincs ilyen foglalás: " + id));
        Long actorId = Authentications.userId(authentication);
        boolean visible = isStaff(booking, authentication) || booking.getCustomer().getId().equals(actorId);
        if (!visible) {
            throw new NotFoundException("Nincs ilyen foglalás: " + id);
        }
        return booking;
    }

    /** A tulajdonos vagy a foglalás borbélya. */
    private static boolean isStaff(Booking booking, Authentication authentication) {
        return Authentications.isOwner(authentication)
                || booking.getBarber().getUser().getId().equals(Authentications.userId(authentication));
    }

    private static void requireBooked(Booking booking) {
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new ConflictException("A foglalás már le van mondva vagy le van zárva.");
        }
    }

    private static Long requireActor(Long actorId) {
        if (actorId == null) {
            throw new AccessDeniedException("Ismeretlen felhasználó.");
        }
        return actorId;
    }
}
