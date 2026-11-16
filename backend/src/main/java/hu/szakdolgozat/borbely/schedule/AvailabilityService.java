package hu.szakdolgozat.borbely.schedule;

import hu.szakdolgozat.borbely.barber.Barber;
import hu.szakdolgozat.borbely.booking.Booking;
import hu.szakdolgozat.borbely.booking.BookingRepository;
import hu.szakdolgozat.borbely.common.BadRequestException;
import hu.szakdolgozat.borbely.common.ConflictException;
import hu.szakdolgozat.borbely.common.NotFoundException;
import hu.szakdolgozat.borbely.schedule.dto.AvailabilityResponse;
import hu.szakdolgozat.borbely.schedule.dto.AvailabilitySlotRequest;
import hu.szakdolgozat.borbely.schedule.dto.AvailabilityUpdateRequest;
import hu.szakdolgozat.borbely.schedule.dto.CopyWeekRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * A munkaidő-sávok kezelése. A szabályok:
 * <ul>
 *   <li>a kezdés és a befejezés negyedórás rácsra esik, és a befejezés későbbi a kezdésnél;</li>
 *   <li>múltbeli napra nem nyitható sáv, és múltbeli sáv nem módosítható, nem törölhető;</li>
 *   <li>egy borbély sávjai nem fedhetik egymást. Ezt az adatbázis {@code availability_no_overlap}
 *       megkötése is kizárja; az alkalmazás előtte azért ellenőrzi, hogy érthető hibaüzenetet adjon;</li>
 *   <li>sávot törölni vagy szűkíteni csak akkor lehet, ha nem esik ki belőle foglalás.</li>
 * </ul>
 */
@Service
public class AvailabilityService {

    private static final Comparator<AvailabilitySlotRequest> CHRONOLOGICAL =
            Comparator.comparing(AvailabilitySlotRequest::workDate).thenComparing(AvailabilitySlotRequest::startTime);

    private final AvailabilityRepository availabilities;
    private final BookingRepository bookings;
    private final ScheduleAccess access;
    private final ScheduleTime time;

    public AvailabilityService(AvailabilityRepository availabilities, BookingRepository bookings,
                               ScheduleAccess access, ScheduleTime time) {
        this.availabilities = availabilities;
        this.bookings = bookings;
        this.access = access;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> list(Long barberId, LocalDate from, LocalDate to, Authentication authentication) {
        access.requireManageable(barberId, authentication);
        ScheduleTime.requireValidRange(from, to);
        return toResponses(findBetween(barberId, from, to));
    }

    /** Több sáv felvétele egy tranzakcióban: ha bármelyik hibás vagy ütközik, egyik sem kerül be. */
    @Transactional
    public List<AvailabilityResponse> create(Long barberId, List<AvailabilitySlotRequest> slots,
                                             Authentication authentication) {
        Barber barber = access.requireManageable(barberId, authentication);
        return toResponses(createSlots(barber, slots));
    }

    /**
     * A forráshét sávjait ugyanarra a napra és időre másolja a célhéten. Ugyanazok a szabályok
     * érvényesek, mint az új sávoknál: ha a célhéten bármelyik ütközik, semmi sem másolódik át.
     */
    @Transactional
    public List<AvailabilityResponse> copyWeek(Long barberId, CopyWeekRequest request, Authentication authentication) {
        Barber barber = access.requireManageable(barberId, authentication);
        LocalDate source = request.sourceWeekStart();
        LocalDate target = request.targetWeekStart();
        if (source.getDayOfWeek() != DayOfWeek.MONDAY || target.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BadRequestException("A hét kezdőnapjának hétfőnek kell lennie.");
        }
        if (source.equals(target)) {
            throw new BadRequestException("A forrás- és a célhét nem lehet ugyanaz.");
        }
        long shiftDays = ChronoUnit.DAYS.between(source, target);
        List<AvailabilitySlotRequest> copies = findBetween(barberId, source, source.plusDays(6)).stream()
                .map(slot -> new AvailabilitySlotRequest(
                        slot.getWorkDate().plusDays(shiftDays), slot.getStartTime(), slot.getEndTime()))
                .toList();
        if (copies.isEmpty()) {
            throw new BadRequestException("A forráshéten nincs munkaidő, nincs mit másolni.");
        }
        return toResponses(createSlots(barber, copies));
    }

    /** Egy sáv kezdésének és befejezésének módosítása; a foglalások nem eshetnek ki belőle. */
    @Transactional
    public AvailabilityResponse update(Long barberId, Long id, AvailabilityUpdateRequest request,
                                       Authentication authentication) {
        access.requireManageable(barberId, authentication);
        Availability slot = find(barberId, id);
        requireNotPast(slot, "Múltbeli munkaidő nem módosítható.");
        requireValidTimes(request.startTime(), request.endTime());

        for (Availability other : findBetween(barberId, slot.getWorkDate(), slot.getWorkDate())) {
            if (!other.getId().equals(id) && overlaps(request.startTime(), request.endTime(), other)) {
                throw new ConflictException("Ütközik egy meglévő munkaidővel: " + describe(other));
            }
        }

        Instant newStart = time.toInstant(slot.getWorkDate(), request.startTime());
        Instant newEnd = time.toInstant(slot.getWorkDate(), request.endTime());
        long outside = bookingsIn(slot).stream()
                .filter(booking -> booking.getStartAt().isBefore(newStart) || booking.getEndAt().isAfter(newEnd))
                .count();
        if (outside > 0) {
            throw new ConflictException("A módosítás után " + outside
                    + " foglalás kívül esne a munkaidőn, ezért így nem menthető.");
        }

        slot.changeTimes(request.startTime(), request.endTime());
        availabilities.flush();
        return AvailabilityResponse.from(slot);
    }

    /** Sáv törlése; csak akkor lehet, ha nem esik bele foglalás. */
    @Transactional
    public void delete(Long barberId, Long id, Authentication authentication) {
        access.requireManageable(barberId, authentication);
        Availability slot = find(barberId, id);
        requireNotPast(slot, "Múltbeli munkaidő nem törölhető.");
        int count = bookingsIn(slot).size();
        if (count > 0) {
            throw new ConflictException("Ebbe a munkaidőbe " + count + " foglalás esik, ezért nem törölhető.");
        }
        availabilities.delete(slot);
    }

    /**
     * Ellenőrzi és elmenti az új sávokat. A kérésen belüli átfedéshez elég időrendbe tenni őket,
     * és csak a szomszédokat összevetni: ha két sáv átfed, a köztük lévő is átfedi az elsőt.
     */
    private List<Availability> createSlots(Barber barber, List<AvailabilitySlotRequest> slots) {
        LocalDate today = time.today();
        for (AvailabilitySlotRequest slot : slots) {
            requireValidTimes(slot.startTime(), slot.endTime());
            if (slot.workDate().isBefore(today)) {
                throw new BadRequestException("Múltbeli napra nem nyitható munkaidő: " + slot.workDate());
            }
        }

        List<AvailabilitySlotRequest> sorted = slots.stream().sorted(CHRONOLOGICAL).toList();
        for (int i = 1; i < sorted.size(); i++) {
            AvailabilitySlotRequest previous = sorted.get(i - 1);
            AvailabilitySlotRequest current = sorted.get(i);
            if (previous.workDate().equals(current.workDate()) && current.startTime().isBefore(previous.endTime())) {
                throw new ConflictException("A megadott munkaidő-sávok között átfedés van: "
                        + describe(current.workDate(), current.startTime(), current.endTime()));
            }
        }

        LocalDate first = sorted.getFirst().workDate();
        LocalDate last = sorted.getLast().workDate();
        List<Availability> existing = findBetween(barber.getId(), first, last);
        for (AvailabilitySlotRequest slot : sorted) {
            for (Availability other : existing) {
                if (other.getWorkDate().equals(slot.workDate()) && overlaps(slot.startTime(), slot.endTime(), other)) {
                    throw new ConflictException("Ütközik egy meglévő munkaidővel: " + describe(other));
                }
            }
        }

        List<Availability> created = sorted.stream()
                .map(slot -> new Availability(barber, slot.workDate(), slot.startTime(), slot.endTime()))
                .toList();
        return availabilities.saveAllAndFlush(created);
    }

    private Availability find(Long barberId, Long id) {
        return availabilities.findByIdAndBarberId(id, barberId)
                .orElseThrow(() -> new NotFoundException("Nincs ilyen munkaidő-sáv: " + id));
    }

    private List<Availability> findBetween(Long barberId, LocalDate from, LocalDate to) {
        return availabilities.findByBarberIdAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(barberId, from, to);
    }

    /** A sávba eső, nem lemondott foglalások. */
    private List<Booking> bookingsIn(Availability slot) {
        return bookings.findActiveOverlapping(
                slot.getBarber().getId(),
                time.toInstant(slot.getWorkDate(), slot.getStartTime()),
                time.toInstant(slot.getWorkDate(), slot.getEndTime()));
    }

    private void requireNotPast(Availability slot, String message) {
        if (slot.getWorkDate().isBefore(time.today())) {
            throw new BadRequestException(message);
        }
    }

    private static void requireValidTimes(LocalTime start, LocalTime end) {
        if (!onQuarterHour(start) || !onQuarterHour(end)) {
            throw new BadRequestException("A munkaidő csak negyedórás pontossággal adható meg (például 9:00, 9:15, 9:30).");
        }
        if (!end.isAfter(start)) {
            throw new BadRequestException("A munkaidő vége legyen későbbi, mint a kezdete.");
        }
    }

    private static boolean onQuarterHour(LocalTime time) {
        return time.getMinute() % 15 == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }

    /** Két, ugyanarra a napra eső sáv átfedi-e egymást. A sávok félig nyitottak: a 9–12 és a 12–14 nem fed át. */
    private static boolean overlaps(LocalTime start, LocalTime end, Availability other) {
        return start.isBefore(other.getEndTime()) && other.getStartTime().isBefore(end);
    }

    private static String describe(Availability slot) {
        return describe(slot.getWorkDate(), slot.getStartTime(), slot.getEndTime());
    }

    private static String describe(LocalDate date, LocalTime start, LocalTime end) {
        return date + " " + start + "–" + end;
    }

    private static List<AvailabilityResponse> toResponses(List<Availability> slots) {
        return slots.stream().map(AvailabilityResponse::from).toList();
    }
}
