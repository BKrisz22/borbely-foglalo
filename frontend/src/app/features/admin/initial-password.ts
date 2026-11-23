/** Kezdőjelszó-generálás az új borbélyhoz; a tulajdonos szóban vagy üzenetben adja át. */

/** Összetéveszthető karakterek (l, I, O, 0, 1) nélkül, hogy könnyű legyen lediktálni. */
const LETTERS = 'abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ';
const DIGITS = '23456789';
const LENGTH = 12;

/** Egyenletes véletlen egész [0, bound) tartományban, kriptográfiai forrásból. */
function secureRandomInt(bound: number): number {
  const limit = Math.floor(0x1_0000_0000 / bound) * bound;
  const buffer = new Uint32Array(1);
  do {
    crypto.getRandomValues(buffer);
  } while (buffer[0] >= limit);
  return buffer[0] % bound;
}

/**
 * 12 karakteres jelszó betűkkel és számjegyekkel; legalább egy betű és egy számjegy biztosan
 * van benne, így megfelel a backend StrongPassword szabályának.
 */
export function generateInitialPassword(
  random: (bound: number) => number = secureRandomInt,
): string {
  const all = LETTERS + DIGITS;
  const chars = [LETTERS[random(LETTERS.length)], DIGITS[random(DIGITS.length)]];
  while (chars.length < LENGTH) {
    chars.push(all[random(all.length)]);
  }
  // Fisher–Yates keverés, hogy a betű és a számjegy ne mindig elöl álljon.
  for (let i = chars.length - 1; i > 0; i--) {
    const j = random(i + 1);
    [chars[i], chars[j]] = [chars[j], chars[i]];
  }
  return chars.join('');
}
