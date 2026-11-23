/**
 * Naptári napok kezelése a böngészőben. A backend a napokat az üzlet helyi idejében
 * (Europe/Budapest) adja "2026-12-01" alakban; a felület ezeket helyi éjfélre eső Date-ként
 * kezeli, így a naptár ugyanazt a napot mutatja, időzónától függetlenül.
 */

function pad(value: number): string {
  return value.toString().padStart(2, '0');
}

/** Date → "2026-12-01" (a helyi naptári nap szerint). */
export function toIsoDate(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** "2026-12-01" → helyi éjfél. */
export function fromIsoDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

/** Érvényes "ÉÉÉÉ-HH-NN" dátum-e (például a címsorból érkező paraméter). */
export function isIsoDate(value: string | null | undefined): value is string {
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return false;
  }
  return toIsoDate(fromIsoDate(value)) === value;
}

export function addDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

export function startOfDay(date: Date = new Date()): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}
