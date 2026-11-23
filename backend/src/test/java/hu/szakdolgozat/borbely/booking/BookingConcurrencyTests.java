package hu.szakdolgozat.borbely.booking;

import hu.szakdolgozat.borbely.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Párhuzamos foglalások: két kérés pontosan egyszerre indul (egy közös „rajtjelre”), és minden
 * körben pontosan az egyiknek kell sikerülnie. Öt kör fut, mindegyik más időpontra.
 */
@IntegrationTest
class BookingConcurrencyTests extends BookingTestSupport {

    private static final int ROUNDS = 5;

    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    @AfterEach
    void stopPool() {
        pool.shutdownNow();
    }

    /** Két vendég ugyanarra a borbélyra és időpontra: az adatbázis kizárási megkötése dönt. */
    @Test
    void twoCustomersCannotBookTheSameSlotAtTheSameTime() throws Exception {
        open(1, day, "09:00", "12:00");

        for (int round = 0; round < ROUNDS; round++) {
            String start = LocalTime.of(9, 0).plusMinutes(30L * round).toString();
            List<Integer> statuses = race(
                    book(1, HAIRCUT, at(day, start), null).with(bearer(vilmaToken)),
                    book(1, HAIRCUT, at(day, start), null).with(bearer(belaToken)));

            assertThat(statuses).as("%d. kör (%s)", round + 1, start).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(countBookings()).isEqualTo(ROUNDS);
    }

    /** Ugyanaz a vendég egyszerre két borbélynál, ugyanarra az időre: a vendég sorának zárolása dönt. */
    @Test
    void oneCustomerCannotBookTwoBarbersAtTheSameTime() throws Exception {
        open(1, day, "09:00", "12:00");
        open(2, day, "09:00", "12:00");

        for (int round = 0; round < ROUNDS; round++) {
            String start = LocalTime.of(9, 0).plusMinutes(30L * round).toString();
            List<Integer> statuses = race(
                    book(1, HAIRCUT, at(day, start), null).with(bearer(vilmaToken)),
                    book(2, HAIRCUT, at(day, start), null).with(bearer(vilmaToken)));

            assertThat(statuses).as("%d. kör (%s)", round + 1, start).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(countBookings()).isEqualTo(ROUNDS);
    }

    /** A két kérést két szálon, egyszerre indítja, és visszaadja a két HTTP-státuszkódot. */
    private List<Integer> race(MockHttpServletRequestBuilder first, MockHttpServletRequestBuilder second)
            throws Exception {
        CountDownLatch startSignal = new CountDownLatch(1);
        Future<Integer> a = pool.submit(send(first, startSignal));
        Future<Integer> b = pool.submit(send(second, startSignal));
        startSignal.countDown();
        return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
    }

    private Callable<Integer> send(MockHttpServletRequestBuilder request, CountDownLatch startSignal) {
        return () -> {
            startSignal.await();
            return mockMvc.perform(request).andReturn().getResponse().getStatus();
        };
    }

    private long countBookings() {
        return jdbc.queryForObject("SELECT count(*) FROM booking", Long.class);
    }
}
