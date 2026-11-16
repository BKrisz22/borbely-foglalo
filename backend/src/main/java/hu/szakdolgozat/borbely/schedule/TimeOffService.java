package hu.szakdolgozat.borbely.schedule;

import hu.szakdolgozat.borbely.barber.Barber;
import hu.szakdolgozat.borbely.booking.BookingRepository;
import hu.szakdolgozat.borbely.common.BadRequestException;
import hu.szakdolgozat.borbely.common.ConflictException;
import hu.szakdolgozat.borbely.common.NotFoundException;
import hu.szakdolgozat.borbely.schedule.dto.TimeOffCreateRequest;
import hu.szakdolgozat.borbely.schedule.dto.TimeOffResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * A távollétek (szabadság, betegség, szünet) kezelése. Egy távollét nem fedhet nem lemondott
 * foglalást és másik távollétet; ha foglalás esik rá, azt előbb le kell mondani.
 * A távollét is negyedórás rácsra esik, hogy a slot-algoritmus egész slotokkal számolhasson.
 */
@Service
public class TimeOffService {

    private static final long QUARTER_HOUR_SECONDS = 15 * 60;

    private final TimeOffRepository timeOffs;
    private final BookingRepository bookings;
    private final ScheduleAccess access;
    private final ScheduleTime time;

    public TimeOffService(TimeOffRepository timeOffs, BookingRepository bookings,
                          ScheduleAccess access, ScheduleTime time) {
        this.timeOffs = timeOffs;
        this.bookings = bookings;
        this.access = access;
        this.time = time;
    }

    /** A két nap (helyi idő szerint, mindkettőt beleértve) közé eső távollétek. */
    @Transactional(readOnly = true)
    public List<TimeOffResponse> list(Long barberId, LocalDate from, LocalDate to, Authentication authentication) {
        access.requireManageable(barberId, authentication);
        ScheduleTime.requireValidRange(from, to);
        return timeOffs.findOverlapping(barberId, time.startOfDay(from), time.startOfDay(to.plusDays(1))).stream()
                .map(TimeOffResponse::from)
                .toList();
    }

    @Transactional
    public TimeOffResponse create(Long barberId, TimeOffCreateRequest request, Authentication authentication) {
        Barber barber = access.requireManageable(barberId, authentication);
        Instant start = request.startAt().toInstant();
        Instant end = request.endAt().toInstant();

        if (!onQuarterHour(start) || !onQuarterHour(end)) {
            throw new BadRequestException("A távollét csak negyedórás pontossággal adható meg (például 12:00, 12:15).");
        }
        if (!end.isAfter(start)) {
            throw new BadRequestException("A távollét vége legyen későbbi, mint a kezdete.");
        }
        if (!end.isAfter(time.now())) {
            throw new BadRequestException("Múltbeli távollét nem vehető fel.");
        }
        if (!timeOffs.findOverlapping(barberId, start, end).isEmpty()) {
            throw new ConflictException("Ütközik egy meglévő távolléttel.");
        }
        int count = bookings.findActiveOverlapping(barberId, start, end).size();
        if (count > 0) {
            throw new ConflictException("Erre az időszakra " + count
                    + " foglalás esik. Előbb a foglalásokat kell lemondani.");
        }

        TimeOff saved = timeOffs.save(new TimeOff(barber, start, end, trimToNull(request.reason())));
        return TimeOffResponse.from(saved);
    }

    /** Távollét törlése; a már lezajlott távollét nem törölhető. */
    @Transactional
    public void delete(Long barberId, Long id, Authentication authentication) {
        access.requireManageable(barberId, authentication);
        TimeOff timeOff = timeOffs.findByIdAndBarberId(id, barberId)
                .orElseThrow(() -> new NotFoundException("Nincs ilyen távollét: " + id));
        if (!timeOff.getEndAt().isAfter(time.now())) {
            throw new BadRequestException("Múltbeli távollét nem törölhető.");
        }
        timeOffs.delete(timeOff);
    }

    /**
     * Negyedórára esik-e az időpont. UTC-ben is elég vizsgálni, mert a budapesti idő
     * egész órával (+1 vagy +2) tér el tőle.
     */
    private static boolean onQuarterHour(Instant instant) {
        return instant.getEpochSecond() % QUARTER_HOUR_SECONDS == 0 && instant.getNano() == 0;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
