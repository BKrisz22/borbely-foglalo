package hu.szakdolgozat.borbely.slot;

import hu.szakdolgozat.borbely.barber.Barber;
import hu.szakdolgozat.borbely.barber.BarberRepository;
import hu.szakdolgozat.borbely.booking.Booking;
import hu.szakdolgozat.borbely.booking.BookingRepository;
import hu.szakdolgozat.borbely.booking.BookingStatus;
import hu.szakdolgozat.borbely.common.BadRequestException;
import hu.szakdolgozat.borbely.schedule.Availability;
import hu.szakdolgozat.borbely.schedule.AvailabilityRepository;
import hu.szakdolgozat.borbely.schedule.ScheduleProperties;
import hu.szakdolgozat.borbely.schedule.ScheduleTime;
import hu.szakdolgozat.borbely.schedule.TimeOff;
import hu.szakdolgozat.borbely.schedule.TimeOffRepository;
import hu.szakdolgozat.borbely.serviceoffering.ServiceOffering;
import hu.szakdolgozat.borbely.slot.dto.SlotResponse;
import hu.szakdolgozat.borbely.user.AppUser;
import hu.szakdolgozat.borbely.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A szabad kezdőpontok számításának szélsőeset-tesztjei, adatbázis nélkül és rögzített órával,
 * így bármelyik napon ugyanúgy futnak. A tárolókat egyszerű, listán dolgozó helyettesítők adják.
 * A munkaidő, a foglalás és a távollét itt mind helyi (budapesti) időben van megadva.
 */
class SlotServiceTest {

    private static final ZoneId BUDAPEST = ZoneId.of("Europe/Budapest");
    private static final long BARBER_ID = 1L;
    private static final long HAIRCUT = 10L;        // 30 perc = 2 slot
    private static final long BEARD = 20L;          // 40 perc = 3 slot
    private static final long FULL_SERVICE = 30L;   // 240 perc = 16 slot

    private final BarberRepository barbers = mock(BarberRepository.class);
    private final AvailabilityRepository availabilities = mock(AvailabilityRepository.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final TimeOffRepository timeOffs = mock(TimeOffRepository.class);

    private final List<Availability> openings = new ArrayList<>();
    private final List<Booking> booked = new ArrayList<>();
    private final List<TimeOff> absences = new ArrayList<>();

    private Barber barber;
    private ServiceOffering haircut;
    private ServiceOffering beard;

    @BeforeEach
    void prepare() {
        barber = new Barber(new AppUser("kovacs.peter@example.com", "!", "Kovács Péter", Role.BARBER), null);
        ReflectionTestUtils.setField(barber, "id", BARBER_ID);
        haircut = service(HAIRCUT, 30);
        beard = service(BEARD, 40);
        barber.addService(haircut);
        barber.addService(beard);
        barber.addService(service(FULL_SERVICE, 240));

        when(barbers.findWithDetailsById(BARBER_ID)).thenReturn(Optional.of(barber));
        when(availabilities.findByBarberIdAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(eq(BARBER_ID), any(), any()))
                .thenAnswer(call -> {
                    LocalDate from = call.getArgument(1);
                    LocalDate to = call.getArgument(2);
                    return openings.stream()
                            .filter(a -> !a.getWorkDate().isBefore(from) && !a.getWorkDate().isAfter(to))
                            .toList();
                });
        when(bookings.findActiveOverlapping(eq(BARBER_ID), any(), any()))
                .thenAnswer(call -> booked.stream()
                        .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                        .filter(b -> overlaps(b.getStartAt(), b.getEndAt(), call.getArgument(1), call.getArgument(2)))
                        .toList());
        when(timeOffs.findOverlapping(eq(BARBER_ID), any(), any()))
                .thenAnswer(call -> absences.stream()
                        .filter(t -> overlaps(t.getStartAt(), t.getEndAt(), call.getArgument(1), call.getArgument(2)))
                        .toList());
    }

    @Test
    void springForwardDayKeepsTheLocalWorkingHours() {
        LocalDate day = LocalDate.of(2027, 3, 28);
        open(day, "09:00", "10:00");

        List<SlotResponse> slots = at("2027-03-20T08:00").findSlots(BARBER_ID, HAIRCUT, day);

        assertThat(times(slots)).containsExactly("09:00", "09:15", "09:30");
        // Nyári időszámítás (UTC+2): a 9:00 helyi idő 7:00 UTC.
        assertThat(slots.getFirst().startAt()).isEqualTo(Instant.parse("2027-03-28T07:00:00Z"));
    }

    @Test
    void theMissingHourOnSpringForwardIsClosed() {
        LocalDate day = LocalDate.of(2027, 3, 28);
        open(day, "01:00", "04:00");

        List<SlotResponse> slots = at("2027-03-20T08:00").findSlots(BARBER_ID, HAIRCUT, day);

        // 2:00 és 3:00 között nincs idő, ezért ott nincs kezdőpont, és foglalás sem nyúlhat át rajta.
        assertThat(times(slots)).containsExactly("01:00", "01:15", "01:30", "03:00", "03:15", "03:30");
        assertThat(slots).extracting(SlotResponse::startAt).doesNotHaveDuplicates();
        assertThat(slots.get(3).startAt()).isEqualTo(Instant.parse("2027-03-28T01:00:00Z"));
    }

    @Test
    void fallBackDayKeepsTheLocalWorkingHours() {
        LocalDate day = LocalDate.of(2027, 10, 31);
        open(day, "09:00", "10:00");

        List<SlotResponse> slots = at("2027-10-25T08:00").findSlots(BARBER_ID, HAIRCUT, day);

        assertThat(times(slots)).containsExactly("09:00", "09:15", "09:30");
        // Téli időszámítás (UTC+1): a 9:00 helyi idő 8:00 UTC.
        assertThat(slots.getFirst().startAt()).isEqualTo(Instant.parse("2027-10-31T08:00:00Z"));
    }

    @Test
    void aStartExactlyAtTheLeadTimeLimitIsOffered() {
        LocalDate day = LocalDate.of(2026, 12, 7);
        open(day, "08:00", "11:00");

        // 8:00-kor, 60 perces foglalási határidővel a legkorábbi kezdőpont pontosan 9:00.
        List<SlotResponse> slots = at("2026-12-07T08:00").findSlots(BARBER_ID, HAIRCUT, day);

        assertThat(times(slots)).first().isEqualTo("09:00");
        assertThat(times(slots)).doesNotContain("08:45");
    }

    @Test
    void startsThatAlreadyPassedTodayAreNotOffered() {
        LocalDate day = LocalDate.of(2026, 12, 7);
        open(day, "08:00", "11:00");
        barber.setMinLeadMinutes(0);

        List<SlotResponse> slots = at("2026-12-07T08:10").findSlots(BARBER_ID, HAIRCUT, day);

        assertThat(times(slots)).first().isEqualTo("08:15");
    }

    @Test
    void aTimeOffSpanningTwoDaysBlocksOnlyItsPartOfTheDay() {
        LocalDate day = LocalDate.of(2026, 12, 7);
        open(day, "09:00", "12:00");
        absences.add(new TimeOff(barber, instant(day.minusDays(1), "20:00"), instant(day, "10:00"), "Betegség"));

        List<SlotResponse> slots = at("2026-12-01T08:00").findSlots(BARBER_ID, HAIRCUT, day);

        assertThat(times(slots)).first().isEqualTo("10:00");
        assertThat(times(slots)).last().isEqualTo("11:30");
    }

    @Test
    void aBookingNotOnTheQuarterHourBlocksTheWholeQuarter() {
        LocalDate day = LocalDate.of(2026, 12, 7);
        open(day, "09:00", "11:00");
        // 40 perces foglalás 9:00-tól 9:40-ig: a 9:30-as slot is foglalt, a következő kezdés 9:45.
        booked.add(new Booking(barber, customer(), beard, instant(day, "09:00")));

        List<SlotResponse> slots = at("2026-12-01T08:00").findSlots(BARBER_ID, HAIRCUT, day);

        assertThat(times(slots)).first().isEqualTo("09:45");
    }

    @Test
    void aServiceLongerThanTheWorkingPeriodHasNoStart() {
        LocalDate day = LocalDate.of(2026, 12, 7);
        open(day, "09:00", "12:00");

        SlotService service = at("2026-12-01T08:00");

        assertThat(service.findSlots(BARBER_ID, FULL_SERVICE, day)).isEmpty();
        assertThat(service.findAvailableDays(BARBER_ID, FULL_SERVICE, day, day.plusDays(6))).isEmpty();
    }

    @Test
    void theBookingWindowEndsThirtyDaysFromToday() {
        LocalDate today = LocalDate.of(2026, 12, 1);
        open(today.plusDays(30), "09:00", "12:00");
        open(today.plusDays(31), "09:00", "12:00");

        SlotService service = at("2026-12-01T08:00");

        assertThat(service.findSlots(BARBER_ID, HAIRCUT, today.plusDays(30))).isNotEmpty();
        assertThat(service.findSlots(BARBER_ID, HAIRCUT, today.plusDays(31))).isEmpty();
    }

    @Test
    void availableDaysSkipPastDaysAndDaysBeyondTheWindow() {
        LocalDate today = LocalDate.of(2026, 12, 1);
        open(today.minusDays(1), "09:00", "12:00");
        open(today.plusDays(30), "09:00", "12:00");
        open(today.plusDays(31), "09:00", "12:00");

        List<LocalDate> days = at("2026-12-01T08:00")
                .findAvailableDays(BARBER_ID, HAIRCUT, today.minusDays(5), today.plusDays(40));

        assertThat(days).containsExactly(today.plusDays(30));
    }

    @Test
    void anInactiveServiceIsRejectedEvenIfTheBarberOffersIt() {
        haircut.setActive(false);

        assertThatThrownBy(() -> at("2026-12-01T08:00").findSlots(BARBER_ID, HAIRCUT, LocalDate.of(2026, 12, 7)))
                .isInstanceOf(BadRequestException.class);
    }

    /** A szolgáltatás a megadott helyi időpontban, a valódi csúszóablakos keresővel. */
    private SlotService at(String localDateTime) {
        Clock clock = Clock.fixed(LocalDateTime.parse(localDateTime).atZone(BUDAPEST).toInstant(), ZoneOffset.UTC);
        ScheduleProperties properties = new ScheduleProperties(BUDAPEST, 30);
        return new SlotService(barbers, availabilities, bookings, timeOffs, new SlidingWindowSlotFinder(),
                new ScheduleTime(clock, properties), properties);
    }

    private void open(LocalDate day, String start, String end) {
        openings.add(new Availability(barber, day, LocalTime.parse(start), LocalTime.parse(end)));
    }

    private static ServiceOffering service(long id, int durationMinutes) {
        ServiceOffering service = new ServiceOffering("Szolgáltatás " + id, null, durationMinutes, 5000);
        ReflectionTestUtils.setField(service, "id", id);
        return service;
    }

    private static AppUser customer() {
        return new AppUser("vendeg@example.com", "!", "Teszt Vendég", Role.CUSTOMER);
    }

    private static Instant instant(LocalDate day, String time) {
        return day.atTime(LocalTime.parse(time)).atZone(BUDAPEST).toInstant();
    }

    private static List<String> times(List<SlotResponse> slots) {
        return slots.stream().map(slot -> slot.time().toString()).toList();
    }

    private static boolean overlaps(Instant start, Instant end, Instant from, Instant to) {
        return start.isBefore(to) && end.isAfter(from);
    }
}
