package hu.szakdolgozat.borbely.demo;

import hu.szakdolgozat.borbely.barber.Barber;
import hu.szakdolgozat.borbely.barber.BarberRepository;
import hu.szakdolgozat.borbely.booking.Booking;
import hu.szakdolgozat.borbely.booking.BookingRepository;
import hu.szakdolgozat.borbely.schedule.Availability;
import hu.szakdolgozat.borbely.schedule.AvailabilityRepository;
import hu.szakdolgozat.borbely.schedule.ScheduleTime;
import hu.szakdolgozat.borbely.serviceoffering.ServiceOffering;
import hu.szakdolgozat.borbely.serviceoffering.ServiceOfferingRepository;
import hu.szakdolgozat.borbely.user.AppUser;
import hu.szakdolgozat.borbely.user.AppUserRepository;
import hu.szakdolgozat.borbely.user.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Bemutató adatok, hogy az alkalmazás üres adatbázis helyett élő adatokkal legyen megmutatható.
 * Csak a {@code demo} profilban fut, és csak akkor, ha még nincs szolgáltatás és borbély az adatbázisban.
 *
 * <p>Indítás a {@code backend} mappából: {@code .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo}
 * (IntelliJ-ben a futtatási beállításnál: Active profiles = demo). Tiszta induláshoz előtte
 * {@code docker compose down -v}, majd {@code docker compose up -d}.
 *
 * <p>Minden bemutató fiók jelszava {@value #PASSWORD}; a tulajdonosi fiók továbbra is a {@code .env}-ből jön.
 * <ul>
 *   <li>borbélyok: kovacs.peter@example.com, szabo.anna@example.com, nagy.david@example.com</li>
 *   <li>vendégek: vilma@example.com, bela@example.com, kata@example.com</li>
 * </ul>
 */
@Component
@Profile("demo")
public class DemoDataInitializer implements ApplicationRunner {

    /** Minden bemutató fiók jelszava. */
    public static final String PASSWORD = "Demo1234";

    /** Ennyi napra előre nyit munkaidőt (a foglalási ablak 30 nap). */
    private static final int DAYS_AHEAD = 21;

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final ServiceOfferingRepository services;
    private final AppUserRepository users;
    private final BarberRepository barbers;
    private final AvailabilityRepository availabilities;
    private final BookingRepository bookings;
    private final PasswordEncoder passwordEncoder;
    private final ScheduleTime time;

    public DemoDataInitializer(ServiceOfferingRepository services, AppUserRepository users, BarberRepository barbers,
                               AvailabilityRepository availabilities, BookingRepository bookings,
                               PasswordEncoder passwordEncoder, ScheduleTime time) {
        this.services = services;
        this.users = users;
        this.barbers = barbers;
        this.availabilities = availabilities;
        this.bookings = bookings;
        this.passwordEncoder = passwordEncoder;
        this.time = time;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (services.count() > 0 || barbers.count() > 0) {
            log.info("Bemutató adatok: az adatbázis nem üres, nem töltök be semmit.");
            return;
        }

        ServiceOffering haircut = service("Hajvágás", "Mosás, vágás, szárítás.", 30, 6500);
        ServiceOffering beard = service("Szakálligazítás", "Formázás géppel és kézzel.", 20, 3500);
        ServiceOffering combo = service("Hajvágás és szakáll", "A kettő együtt, kedvezményes áron.", 45, 9000);
        ServiceOffering kids = service("Gyerekhajvágás", "12 éves korig.", 30, 4500);

        Barber peter = barber("kovacs.peter@example.com", "Kovács Péter",
                "Tíz éve borbély, a klasszikus vágások specialistája.", haircut, beard, combo, kids);
        Barber anna = barber("szabo.anna@example.com", "Szabó Anna",
                "Modern frizurák és szakállformázás.", haircut, beard, combo);
        Barber david = barber("nagy.david@example.com", "Nagy Dávid",
                "Türelmes, gyerekbarát, gyors.", haircut, kids);

        AppUser vilma = customer("vilma@example.com", "Vendég Vilma");
        AppUser bela = customer("bela@example.com", "Teszt Béla");
        AppUser kata = customer("kata@example.com", "Kiss Kata");

        // Munkaidő: Péter és Anna hétfőtől péntekig, Dávid keddtől szombatig.
        LocalDate today = time.today();
        int periods = 0;
        for (int i = 0; i <= DAYS_AHEAD; i++) {
            LocalDate day = today.plusDays(i);
            int weekday = day.getDayOfWeek().getValue();
            if (weekday <= 5) {
                periods += open(peter, day, "09:00", "12:00");
                periods += open(peter, day, "13:00", "18:00");
                periods += open(anna, day, "10:00", "18:00");
            }
            if (weekday >= 2 && weekday <= 6) {
                periods += open(david, day, "09:00", "14:00");
            }
        }

        // Néhány jövőbeli foglalás a következő munkanapokra.
        LocalDate first = nextWeekday(today.plusDays(1));
        LocalDate second = nextWeekday(first.plusDays(1));
        book(peter, vilma, haircut, first, "10:00");
        book(anna, bela, combo, first, "14:00");
        book(peter, kata, beard, second, "09:30");

        // Lezárt foglalások a múltból, hogy a listákban előzmény is legyen.
        book(peter, vilma, haircut, today.minusDays(7), "11:00").complete();
        book(anna, bela, beard, today.minusDays(3), "15:00").markNoShow();

        log.info("Bemutató adatok betöltve: 4 szolgáltatás, 3 borbély, 3 vendég, {} munkaidő-sáv, 5 foglalás. "
                + "Minden bemutató fiók jelszava: {}", periods, PASSWORD);
    }

    private ServiceOffering service(String name, String description, int minutes, int priceHuf) {
        return services.save(new ServiceOffering(name, description, minutes, priceHuf));
    }

    private Barber barber(String email, String fullName, String bio, ServiceOffering... offered) {
        AppUser user = users.save(new AppUser(email, passwordEncoder.encode(PASSWORD), fullName, Role.BARBER));
        Barber barber = new Barber(user, bio);
        for (ServiceOffering service : offered) {
            barber.addService(service);
        }
        return barbers.save(barber);
    }

    private AppUser customer(String email, String fullName) {
        return users.save(new AppUser(email, passwordEncoder.encode(PASSWORD), fullName, Role.CUSTOMER));
    }

    private int open(Barber barber, LocalDate day, String start, String end) {
        availabilities.save(new Availability(barber, day, LocalTime.parse(start), LocalTime.parse(end)));
        return 1;
    }

    private Booking book(Barber barber, AppUser customer, ServiceOffering service, LocalDate day, String start) {
        return bookings.save(new Booking(barber, customer, service, time.toInstant(day, LocalTime.parse(start))));
    }

    private static LocalDate nextWeekday(LocalDate day) {
        LocalDate result = day;
        while (result.getDayOfWeek() == DayOfWeek.SATURDAY || result.getDayOfWeek() == DayOfWeek.SUNDAY) {
            result = result.plusDays(1);
        }
        return result;
    }
}
