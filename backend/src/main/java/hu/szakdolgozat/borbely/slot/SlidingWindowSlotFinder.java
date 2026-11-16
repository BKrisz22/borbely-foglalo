package hu.szakdolgozat.borbely.slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Csúszóablakos keresés: egy k slot széles ablakot léptet végig a napon, és egy futó számlálóban
 * tartja nyilván, hány szabad slot van benne. Egy lépésnél csak a kilépő és a belépő slotot kell
 * megnézni, ezért minden slotot legfeljebb kétszer vizsgál, és a futásidő O(n), k-tól függetlenül.
 *
 * <p>Források:
 * <ul>
 *   <li>A. Laaksonen: Competitive Programmer's Handbook, 2018, 8. fejezet („Amortized analysis”),
 *       8.1 „Two pointers method” és 8.3 „Sliding window minimum”, https://cses.fi/book/book.pdf.
 *       Innen a csúszóablak fogalma (állandó méretű résztömb, amely balról jobbra halad a tömbön),
 *       és az, hogy a csak előre mozgó mutatók miatt az egész bejárás O(n) lépés.</li>
 *   <li>GeeksforGeeks: „Sliding Window Technique”, utolsó frissítés: 2026. április 1.,
 *       https://www.geeksforgeeks.org/dsa/window-sliding-technique/ (megtekintve: 2026. november 16.).
 *       Innen a rögzített méretű ablak lépései: az első k elem összege, majd lépésenként a kilépő
 *       elem levonása és a belépő hozzáadása.</li>
 * </ul>
 * Átdolgozás: a forrásbeli összeg helyett a szabad slotok számát tartja nyilván (szabad = 1,
 * foglalt = 0), és a legnagyobb összeg keresése helyett minden olyan kezdőpontot visszaad,
 * ahol ez a szám eléri k-t, vagyis az ablak minden slotja szabad.
 */
public class SlidingWindowSlotFinder implements SlotFinder {

    @Override
    public List<Integer> findStarts(boolean[] free, int k) {
        Objects.requireNonNull(free, "A slotok tömbje nem lehet null.");
        if (k < 1) {
            throw new IllegalArgumentException("A szükséges slotok száma legalább 1 legyen, kapott érték: " + k);
        }

        List<Integer> starts = new ArrayList<>();
        int n = free.length;
        if (k > n) {
            return starts;
        }

        // Az első ablak (0 … k−1): hány szabad slot van benne.
        int freeInWindow = 0;
        for (int i = 0; i < k; i++) {
            if (free[i]) {
                freeInWindow++;
            }
        }

        for (int start = 0; start <= n - k; start++) {
            if (start > 0) {
                // Az ablak eggyel jobbra lép: a start−1. slot kilép, a start+k−1. belép.
                if (free[start - 1]) {
                    freeInWindow--;
                }
                if (free[start + k - 1]) {
                    freeInWindow++;
                }
            }
            if (freeInWindow == k) {
                starts.add(start);
            }
        }
        return starts;
    }
}
