package hu.szakdolgozat.borbely.slot;

import java.util.List;

/**
 * Szabad kezdőpontok keresése egy nap slotjai között. Tisztán algoritmikus, Spring-független
 * feladat, ezért egységtesztekkel önmagában ellenőrizhető.
 *
 * <p>Két megvalósítása van. A {@code NaiveSlotFinder} minden lehetséges kezdőpontnál újra
 * megvizsgálja a következő k slotot (O(n·k)). A {@code SlidingWindowSlotFinder} egy k hosszú
 * csúszóablakkal és futó számlálóval egyetlen bejárással dolgozik (O(n)). A kettőnek minden
 * bemenetre ugyanazt kell visszaadnia; éles működésben a csúszóablakos fut.
 */
public interface SlotFinder {

    /**
     * Megkeresi azokat a kezdőpontokat, ahonnan k egymást követő szabad slot indul,
     * vagyis az i, i+1, …, i+k−1 slot mind szabad.
     *
     * @param free egy nap slotjai időrendben; {@code free[i]} igaz, ha az i-edik slot szabad
     * @param k    hány egymást követő szabad slot kell a szolgáltatáshoz; legalább 1
     * @return a megfelelő i indexek növekvő sorrendben; üres lista, ha nincs ilyen
     *         (például ha k nagyobb a tömb hosszánál)
     * @throws NullPointerException     ha {@code free} null
     * @throws IllegalArgumentException ha {@code k < 1}
     */
    List<Integer> findStarts(boolean[] free, int k);
}
