package hu.szakdolgozat.borbely.slot;

import hu.szakdolgozat.borbely.barber.Barber;
import hu.szakdolgozat.borbely.barber.BarberRepository;
import hu.szakdolgozat.borbely.booking.Booking;
import hu.szakdolgozat.borbely.booking.BookingRepository;
import hu.szakdolgozat.borbely.common.BadRequestException;
import hu.szakdolgozat.borbely.common.NotFoundException;
import hu.szakdolgozat.borbely.schedule.Availability;
import hu.szakdolgozat.borbely.schedule.AvailabilityRepository;
import hu.szakdolgozat.borbely.schedule.ScheduleProperties;
import hu.szakdolgozat.borbely.schedule.ScheduleTime;
import hu.szakdolgozat.borbely.schedule.TimeOff;
import hu.szakdolgozat.borbely.schedule.TimeOffRepository;
import hu.szakdolgozat.borbely.serviceoffering.ServiceOffering;
import hu.szakdolgozat.borbely.slot.dto.SlotResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A foglalható kezdőpontok kiszámítása:
 * <ol>
 *   <li>a nap munkaidő-sávjai megnyitják a 15 perces slotokat;</li>
 *   <li>a nem lemondott foglalások és a távollétek lezárják azokat, amelyekbe belelógnak;</li>
 *   <li>a szolgáltatáshoz k = ⌈időtartam / 15⌉ egymást követő szabad slot kell; ezeket
 *       a {@link SlotFinder} keresi meg;</li>
 *   <li>kiesnek a borbély foglalási határidején belüli kezdőpontok, és csak ma és
 *       ma + N nap (app.schedule.booking-horizon-days) közötti napra lehet foglalni.</li>
 * </ol>
 * A két munkaidő-sáv közötti szünet zárt slot marad, így egy foglalás nem nyúlhat át rajta.
 * Ugyanígy zárt a tavaszi óraátállításkor kimaradó óra is.
 */
@Service
public class SlotService {

    private final BarberRepository barbers;
    private final AvailabilityRepository availabilities;
    private final BookingRepository bookings;
    private final TimeOffRepository timeOffs;
    private final SlotFinder finder;
    private final ScheduleTime time;
    private final ScheduleProperties properties;

    public SlotService(BarberRepository barbers, AvailabilityRepository availabilities, BookingRepository bookings,
                       TimeOffRepository timeOffs, SlotFinder finder, ScheduleTime time,
                       ScheduleProperties properties) {
        this.barbers = barbers;
        this.availabilities = availabilities;
        this.bookings = bookings;
        this.timeOffs = timeOffs;
        this.finder = finder;
        this.time = time;
        this.properties = properties;
    }

    /** Egy nap foglalható kezdőpontjai egy szolgáltatáshoz, időrendben. */
    @Transactional(readOnly = true)
    public List<SlotResponse> findSlots(Long barberId, Long serviceId, LocalDate date) {
        Barber barber = requireActiveBarber(barberId);
        ServiceOffering service = requireOffered(barber, serviceId);
        LocalDate today = time.today();
        if (date.isBefore(today) || date.isAfter(lastBookableDay(today))) {
            return List.of();
        }
        Window window = load(barberId, date, date);
        return startsOn(barber, service, date, window, earliestStart(barber)).stream()
                .map(start -> new SlotResponse(start, time.localTime(start)))
                .toList();
    }

    /**
     * Azok a napok két dátum között, amelyeken van legalább egy foglalható kezdőpont.
     * A foglalási tölcsér naptára csak ezeket kínálja fel.
     */
    @Transactional(readOnly = true)
    public List<LocalDate> findAvailableDays(Long barberId, Long serviceId, LocalDate from, LocalDate to) {
        ScheduleTime.requireValidRange(from, to);
        Barber barber = requireActiveBarber(barberId);
        ServiceOffering service = requireOffered(barber, serviceId);
        LocalDate today = time.today();
        LocalDate first = from.isBefore(today) ? today : from;
        LocalDate last = to.isAfter(lastBookableDay(today)) ? lastBookableDay(today) : to;
        if (first.isAfter(last)) {
            return List.of();
        }
        Window window = load(barberId, first, last);
        Instant earliest = earliestStart(barber);
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            if (!startsOn(barber, service, day, window, earliest).isEmpty()) {
                days.add(day);
            }
        }
        return days;
    }

    /**
     * Foglalható-e most a megadott kezdőpont: a foglalási ablakon belüli napra esik, és a
     * slot-algoritmus is felkínálná. A vendég saját foglalásánál a borbély foglalási határideje is
     * számít ({@code applyLeadTime}); a borbély vagy a tulajdonos, például telefonos foglalásnál,
     * a kezdésig felvehet foglalást. A foglalás tranzakciójában fut, ezért nem nyit sajátot.
     */
    public boolean isBookableStart(Barber barber, ServiceOffering service, Instant startAt, boolean applyLeadTime) {
        LocalDate date = time.localDate(startAt);
        LocalDate today = time.today();
        if (date.isBefore(today) || date.isAfter(lastBookableDay(today))) {
            return false;
        }
        Instant earliest = applyLeadTime ? earliestStart(barber) : time.now();
        return startsOn(barber, service, date, load(barber.getId(), date, date), earliest).contains(startAt);
    }

    /** Egy nap foglalható kezdőpontjai UTC-ben; a legkorábbi elfogadott kezdés {@code earliest}. */
    private List<Instant> startsOn(Barber barber, ServiceOffering service, LocalDate date, Window window,
                                   Instant earliest) {
        List<Availability> open = window.availability().getOrDefault(date, List.of());
        if (open.isEmpty()) {
            return List.of();
        }
        SlotGrid grid = new SlotGrid();
        for (Availability slot : open) {
            grid.open(minuteOf(slot.getStartTime()), minuteOf(slot.getEndTime()));
        }
        closeNonexistentTimes(grid, date);
        Instant dayStart = time.startOfDay(date);
        Instant dayEnd = time.startOfDay(date.plusDays(1));
        for (Booking booking : window.bookings()) {
            block(grid, date, dayStart, dayEnd, booking.getStartAt(), booking.getEndAt());
        }
        for (TimeOff timeOff : window.timeOffs()) {
            block(grid, date, dayStart, dayEnd, timeOff.getStartAt(), timeOff.getEndAt());
        }

        int k = SlotGrid.slotsNeeded(service.getDurationMinutes());
        return finder.findStarts(grid.free(), k).stream()
                .map(index -> time.toInstant(date, SlotGrid.startOf(index)))
                .filter(start -> !start.isBefore(earliest))
                .toList();
    }

    /**
     * A tavaszi óraátállítás napján helyi idő szerint 2:00 és 3:00 között nincs idő. Ezek a slotok
     * zártak maradnak, különben 3:00-ra csúsznának, és ugyanaz a kezdőpont kétszer szerepelne.
     * Az őszi átállításkor kétszer előforduló órát a rács egyszer, az első előfordulásával kezeli;
     * egy borbélyüzlet ebben az órában nem dolgozik.
     */
    private void closeNonexistentTimes(SlotGrid grid, LocalDate date) {
        for (int i = 0; i < SlotGrid.SLOTS_PER_DAY; i++) {
            if (!time.exists(date, SlotGrid.startOf(i))) {
                grid.block(i * SlotGrid.SLOT_MINUTES, (i + 1) * SlotGrid.SLOT_MINUTES);
            }
        }
    }

    /** Lezárja a rácson egy foglalás vagy távollét napra eső részét. */
    private void block(SlotGrid grid, LocalDate date, Instant dayStart, Instant dayEnd, Instant start, Instant end) {
        if (start.isBefore(dayEnd) && end.isAfter(dayStart)) {
            grid.block(time.minuteOfDay(date, start), time.minuteOfDay(date, end));
        }
    }

    /** Az időszak munkaidő-sávjai, foglalásai és távollétei, három lekérdezéssel. */
    private Window load(Long barberId, LocalDate first, LocalDate last) {
        Instant from = time.startOfDay(first);
        Instant to = time.startOfDay(last.plusDays(1));
        Map<LocalDate, List<Availability>> availability = availabilities
                .findByBarberIdAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(barberId, first, last).stream()
                .collect(Collectors.groupingBy(Availability::getWorkDate));
        return new Window(availability,
                bookings.findActiveOverlapping(barberId, from, to),
                timeOffs.findOverlapping(barberId, from, to));
    }

    /** A vendég legkorábban ennyi idő múlva kezdődő időpontot foglalhat: most + a borbély foglalási határideje. */
    private Instant earliestStart(Barber barber) {
        return time.now().plus(Duration.ofMinutes(barber.getMinLeadMinutes()));
    }

    private LocalDate lastBookableDay(LocalDate today) {
        return today.plusDays(properties.bookingHorizonDays());
    }

    /** A kikapcsolt borbély a vendégnek nem létezik, ahogy a nyilvános borbélylistában sem. */
    public Barber requireActiveBarber(Long barberId) {
        return barbers.findWithDetailsById(barberId)
                .filter(Barber::isActive)
                .orElseThrow(() -> new NotFoundException("Nincs ilyen borbély: " + barberId));
    }

    /** A borbély által vállalt, aktív szolgáltatás; ha nincs ilyen, 400. */
    public static ServiceOffering requireOffered(Barber barber, Long serviceId) {
        return barber.getServices().stream()
                .filter(service -> service.getId().equals(serviceId) && service.isActive())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Ez a borbély nem vállal ilyen szolgáltatást: " + serviceId));
    }

    private static int minuteOf(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }

    /** Egy időszak adatai, hogy a napok kiszámításához ne kelljen naponta lekérdezni. */
    private record Window(Map<LocalDate, List<Availability>> availability,
                          List<Booking> bookings,
                          List<TimeOff> timeOffs) {
    }
}
