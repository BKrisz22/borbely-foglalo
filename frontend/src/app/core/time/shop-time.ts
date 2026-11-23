/**
 * Időpontok megjelenítése az üzlet idejében (Europe/Budapest), a böngésző időzónájától
 * függetlenül. A backend a foglalásokat UTC-ben adja ("2026-11-26T09:00:00Z").
 */
export const SHOP_TIME_ZONE = 'Europe/Budapest';

const dateFormat = new Intl.DateTimeFormat('hu-HU', {
  timeZone: SHOP_TIME_ZONE,
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long',
});

const timeFormat = new Intl.DateTimeFormat('hu-HU', {
  timeZone: SHOP_TIME_ZONE,
  hour: '2-digit',
  minute: '2-digit',
});

/** "2026. november 26., csütörtök" */
export function shopDate(instant: string): string {
  return dateFormat.format(new Date(instant));
}

/** "10:00" */
export function shopTime(instant: string): string {
  return timeFormat.format(new Date(instant));
}
