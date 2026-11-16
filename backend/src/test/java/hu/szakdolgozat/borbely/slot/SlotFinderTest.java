package hu.szakdolgozat.borbely.slot;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A két keresőalgoritmus közös egységtesztjei: minden eset mindkét megvalósításon lefut.
 * A mintákban a T szabad, az F foglalt vagy zárt slotot jelöl.
 */
class SlotFinderTest {

    static Stream<Arguments> finders() {
        return Stream.of(
                Arguments.of(Named.of("naiv", new NaiveSlotFinder())),
                Arguments.of(Named.of("csúszóablak", new SlidingWindowSlotFinder())));
    }

    @ParameterizedTest
    @MethodSource("finders")
    void designDocumentExample(SlotFinder finder) {
        // 9:00–12:00 = 12 slot, foglalás 10:00–10:30 (4. és 5. slot), 40 perces szolgáltatás → k = 3
        assertThat(finder.findStarts(slots("TTTTFFTTTTTT"), 3)).containsExactly(0, 1, 6, 7, 8, 9);
    }

    @ParameterizedTest
    @MethodSource("finders")
    void kOfOneReturnsEveryFreeSlot(SlotFinder finder) {
        assertThat(finder.findStarts(slots("TFTTFT"), 1)).containsExactly(0, 2, 3, 5);
    }

    @ParameterizedTest
    @MethodSource("finders")
    void allFreeGivesEveryStartThatFits(SlotFinder finder) {
        assertThat(finder.findStarts(slots("TTTTT"), 2)).containsExactly(0, 1, 2, 3);
    }

    @ParameterizedTest
    @MethodSource("finders")
    void windowAsLongAsTheDay(SlotFinder finder) {
        assertThat(finder.findStarts(slots("TTT"), 3)).containsExactly(0);
    }

    @ParameterizedTest
    @MethodSource("finders")
    void kLongerThanTheDayGivesNothing(SlotFinder finder) {
        assertThat(finder.findStarts(slots("TTT"), 4)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("finders")
    void emptyDayGivesNothing(SlotFinder finder) {
        assertThat(finder.findStarts(new boolean[0], 1)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("finders")
    void fullyBookedDayGivesNothing(SlotFinder finder) {
        assertThat(finder.findStarts(slots("FFFF"), 1)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("finders")
    void runsShorterThanKAreSkipped(SlotFinder finder) {
        assertThat(finder.findStarts(slots("TTFTTFTTT"), 3)).containsExactly(6);
    }

    @ParameterizedTest
    @MethodSource("finders")
    void freeRunAtTheVeryStartAndEnd(SlotFinder finder) {
        assertThat(finder.findStarts(slots("TTFFFTT"), 2)).containsExactly(0, 5);
    }

    @ParameterizedTest
    @MethodSource("finders")
    void invalidArgumentsAreRejected(SlotFinder finder) {
        assertThatThrownBy(() -> finder.findStarts(null, 1)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> finder.findStarts(slots("TT"), 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> finder.findStarts(slots("TT"), -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("finders")
    void inputArrayIsNotModified(SlotFinder finder) {
        boolean[] free = slots("TFTTF");
        finder.findStarts(free, 2);
        assertThat(free).containsExactly(slots("TFTTF"));
    }

    /** Ezer véletlenszerű napon (rögzített kezdőértékkel) a két megvalósítás ugyanazt adja. */
    @Test
    void bothImplementationsAgreeOnRandomDays() {
        Random random = new Random(42);
        SlotFinder naive = new NaiveSlotFinder();
        SlotFinder slidingWindow = new SlidingWindowSlotFinder();
        for (int round = 0; round < 1000; round++) {
            boolean[] free = new boolean[random.nextInt(SlotGrid.SLOTS_PER_DAY + 1)];
            for (int i = 0; i < free.length; i++) {
                free[i] = random.nextDouble() < 0.7;
            }
            int k = 1 + random.nextInt(12);
            assertThat(slidingWindow.findStarts(free, k))
                    .as("k = %d, slotok = %s", k, Arrays.toString(free))
                    .isEqualTo(naive.findStarts(free, k));
        }
    }

    private static boolean[] slots(String pattern) {
        boolean[] free = new boolean[pattern.length()];
        for (int i = 0; i < free.length; i++) {
            free[i] = pattern.charAt(i) == 'T';
        }
        return free;
    }
}
