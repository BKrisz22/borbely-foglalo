package hu.szakdolgozat.borbely.slot;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A napi slotrács egységtesztjei: megnyitás, lezárás részleges átfedésnél, a szükséges slotok száma.
 */
class SlotGridTest {

    @Test
    void openingAWorkingPeriodFreesExactlyItsSlots() {
        SlotGrid grid = new SlotGrid();
        grid.open(minute(9, 0), minute(12, 0));

        boolean[] free = grid.free();
        assertThat(free).hasSize(96);
        assertThat(free[35]).isFalse();   // 8:45
        assertThat(free[36]).isTrue();    // 9:00
        assertThat(free[47]).isTrue();    // 11:45
        assertThat(free[48]).isFalse();   // 12:00
    }

    @Test
    void blockingClosesEverySlotItTouches() {
        SlotGrid grid = new SlotGrid();
        grid.open(minute(9, 0), minute(12, 0));
        // 9:00-kor kezdődő 40 perces foglalás: a 9:00, 9:15 és 9:30 slot is foglalt lesz
        grid.block(minute(9, 0), minute(9, 40));

        boolean[] free = grid.free();
        assertThat(free[36]).isFalse();
        assertThat(free[37]).isFalse();
        assertThat(free[38]).isFalse();
        assertThat(free[39]).isTrue();    // 9:45
    }

    @Test
    void blockingTheWholeDayClosesEverything() {
        SlotGrid grid = new SlotGrid();
        grid.open(0, 24 * 60);
        grid.block(0, 24 * 60);

        assertThat(grid.free()).containsOnly(false);
    }

    @Test
    void slotsNeededRoundsUp() {
        assertThat(SlotGrid.slotsNeeded(1)).isEqualTo(1);
        assertThat(SlotGrid.slotsNeeded(15)).isEqualTo(1);
        assertThat(SlotGrid.slotsNeeded(30)).isEqualTo(2);
        assertThat(SlotGrid.slotsNeeded(40)).isEqualTo(3);
        assertThat(SlotGrid.slotsNeeded(45)).isEqualTo(3);
        assertThat(SlotGrid.slotsNeeded(480)).isEqualTo(32);
    }

    @Test
    void slotIndexMapsToLocalTime() {
        assertThat(SlotGrid.startOf(0)).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(SlotGrid.startOf(37)).isEqualTo(LocalTime.of(9, 15));
        assertThat(SlotGrid.startOf(95)).isEqualTo(LocalTime.of(23, 45));
    }

    private static int minute(int hour, int minute) {
        return hour * 60 + minute;
    }
}
