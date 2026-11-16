package hu.szakdolgozat.borbely.slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Naiv keresés: minden lehetséges kezdőpontnál újra végignézi a következő k slotot.
 * Futásideje legrosszabb esetben (például egy teljesen szabad napon) O(n·k), ahol n a slotok száma.
 * Az éles rendszer a {@link SlidingWindowSlotFinder}-t használja; ez az osztály a tesztekben
 * ellenőrzésként és az összehasonlító mérésben szerepel.
 *
 * <p>Forrás: a k egymást követő elem összegét kereső feladat naiv megoldása („Naive Approach –
 * O(n×k) Time”): GeeksforGeeks, „Sliding Window Technique”, utolsó frissítés: 2026. április 1.,
 * https://www.geeksforgeeks.org/dsa/window-sliding-technique/ (megtekintve: 2026. november 16.).
 * Átdolgozás: az elemek összegzése helyett azt vizsgálja, hogy az ablak minden slotja szabad-e;
 * az első foglalt slotnál abbahagyja a vizsgálatot, és nem a maximumot, hanem minden megfelelő
 * kezdőpontot visszaad.
 */
public class NaiveSlotFinder implements SlotFinder {

    @Override
    public List<Integer> findStarts(boolean[] free, int k) {
        Objects.requireNonNull(free, "A slotok tömbje nem lehet null.");
        if (k < 1) {
            throw new IllegalArgumentException("A szükséges slotok száma legalább 1 legyen, kapott érték: " + k);
        }

        List<Integer> starts = new ArrayList<>();
        // Kezdőpont csak ott lehet, ahonnan még k slot belefér a napba (k > n esetén egy sem).
        for (int start = 0; start <= free.length - k; start++) {
            boolean allFree = true;
            for (int i = start; i < start + k; i++) {
                if (!free[i]) {
                    allFree = false;
                    break;
                }
            }
            if (allFree) {
                starts.add(start);
            }
        }
        return starts;
    }
}
