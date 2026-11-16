package hu.szakdolgozat.borbely.slot;

import java.time.LocalTime;

/**
 * Egy nap 15 perces slotjai helyi idő szerint, 0:00-tól (96 darab). Kezdetben minden slot zárt;
 * a munkaidő-sávok megnyitják, a foglalások és a távollétek lezárják őket. Az időpontokat
 * a nap kezdete óta eltelt percekben kapja (0–1440).
 */
final class SlotGrid {

    static final int SLOT_MINUTES = 15;
    static final int SLOTS_PER_DAY = 24 * 60 / SLOT_MINUTES;

    private final boolean[] free = new boolean[SLOTS_PER_DAY];

    /** Megnyitja a [from, to) percek közé eső slotokat. A munkaidő negyedórás rácsra esik. */
    void open(int fromMinute, int toMinute) {
        for (int i = clamp(fromMinute / SLOT_MINUTES); i < clamp(toMinute / SLOT_MINUTES); i++) {
            free[i] = true;
        }
    }

    /**
     * Lezár minden slotot, amelybe a [from, to) időszak akár csak részben belelóg. Egy 9:00-kor
     * kezdődő 40 perces foglalás így a 9:00, a 9:15 és a 9:30 slotot is lefoglalja.
     */
    void block(int fromMinute, int toMinute) {
        for (int i = clamp(fromMinute / SLOT_MINUTES); i < clamp(Math.ceilDiv(toMinute, SLOT_MINUTES)); i++) {
            free[i] = false;
        }
    }

    /** A slotok állapota másolatként, hogy a hívó ne írhassa át a rácsot. */
    boolean[] free() {
        return free.clone();
    }

    /** Hány slot kell egy adott hosszúságú szolgáltatáshoz: ⌈időtartam / 15⌉ (40 perc → 3). */
    static int slotsNeeded(int durationMinutes) {
        return Math.ceilDiv(durationMinutes, SLOT_MINUTES);
    }

    /** Az i-edik slot kezdete helyi időben (például 37 → 9:15). */
    static LocalTime startOf(int index) {
        return LocalTime.MIN.plusMinutes((long) index * SLOT_MINUTES);
    }

    private static int clamp(int index) {
        return Math.max(0, Math.min(SLOTS_PER_DAY, index));
    }
}
