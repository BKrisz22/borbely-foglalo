/**
 * A borbély foglalási határideje: legalább hány perccel a kezdés előtt foglalhat a vendég
 * (0–1440 perc, a backend BarberProfileRequest szabálya szerint).
 */
export const LEAD_TIME_MAX = 1440;

/** A választható értékek; ha a mentett érték ezek között nincs, a lista kiegészül vele. */
export const LEAD_TIME_PRESETS: readonly number[] = [0, 15, 30, 60, 120, 180, 360, 720, 1440];

/** "Nincs határidő", "30 perc", "2 óra", "1 óra 30 perc", "1 nap" */
export function leadTimeLabel(minutes: number): string {
  if (minutes <= 0) {
    return 'Nincs határidő';
  }
  if (minutes % 1440 === 0) {
    return `${minutes / 1440} nap`;
  }
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  if (hours === 0) {
    return `${rest} perc`;
  }
  return rest === 0 ? `${hours} óra` : `${hours} óra ${rest} perc`;
}

/** A választható értékek növekvő sorrendben, a jelenlegi értékkel kiegészítve. */
export function leadTimeOptions(current?: number | null): number[] {
  const options = new Set(LEAD_TIME_PRESETS);
  if (current !== undefined && current !== null) {
    options.add(current);
  }
  return [...options].sort((a, b) => a - b);
}
