package hu.szakdolgozat.borbely.schedule;

import hu.szakdolgozat.borbely.common.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Átváltás a borbélyok helyi ideje és a tárolt UTC-időpontok között. Mivel a munkaidő helyi időben
 * van megadva, a 2027. március 28-i óraátállítás napján a 9:00–17:00-s sáv is 9:00–17:00 marad.
 * Az órát ({@link Clock}) beanként kapja, így tesztben rögzíthető.
 */
@Component
public class ScheduleTime {

    /** Egyszerre legfeljebb ennyi napot lehet lekérdezni (nagyjából három hónapot). */
    public static final int MAX_RANGE_DAYS = 93;

    private final Clock clock;
    private final ZoneId zone;

    public ScheduleTime(Clock clock, ScheduleProperties properties) {
        this.clock = clock;
        this.zone = properties.zone();
    }

    /** A mai nap helyi idő szerint. */
    public LocalDate today() {
        return LocalDate.now(clock.withZone(zone));
    }

    /** A jelenlegi időpont. */
    public Instant now() {
        return clock.instant();
    }

    /** Egy helyi nap és időpont UTC-ben. */
    public Instant toInstant(LocalDate date, LocalTime time) {
        return date.atTime(time).atZone(zone).toInstant();
    }

    /** A nap kezdete (helyi éjfél) UTC-ben. */
    public Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(zone).toInstant();
    }

    /** Egy időpont helyi idő szerint (például a 08:15Z télen 9:15). */
    public LocalTime localTime(Instant instant) {
        return LocalTime.ofInstant(instant, zone);
    }

    /**
     * Létezik-e ez a helyi időpont az adott napon. A tavaszi óraátállítás napján (például
     * 2027. március 28-án) helyi idő szerint 2:00 és 3:00 között nincs idő.
     */
    public boolean exists(LocalDate date, LocalTime time) {
        return !zone.getRules().getValidOffsets(date.atTime(time)).isEmpty();
    }

    /**
     * Hány perc telt el helyi idő szerint a nap kezdete óta az adott időpontig. A napnál korábbi
     * időpontra 0, a napnál későbbire 1440 jön ki, így a több napon átnyúló távollét is a napra vágható.
     */
    public int minuteOfDay(LocalDate date, Instant instant) {
        ZonedDateTime local = instant.atZone(zone);
        if (local.toLocalDate().isBefore(date)) {
            return 0;
        }
        if (local.toLocalDate().isAfter(date)) {
            return 24 * 60;
        }
        return local.getHour() * 60 + local.getMinute();
    }

    /** Ellenőrzi a lekérdezett időszakot: a vége nem lehet korábbi a kezdeténél, és nem lehet túl hosszú. */
    public static void requireValidRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BadRequestException("A záró dátum nem lehet korábbi a kezdő dátumnál.");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
            throw new BadRequestException("Egyszerre legfeljebb " + MAX_RANGE_DAYS + " nap kérdezhető le.");
        }
    }
}
