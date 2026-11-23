import type { EventInput } from '@fullcalendar/angular';

import {
  Availability,
  AvailabilitySlotRequest,
  Booking,
  BookingStatus,
  TimeOff,
} from '../../core/api/api.models';
import { addDays, fromIsoDate, toIsoDate } from '../../core/time/local-date';
import { shopOffset } from '../../core/time/shop-time';
import { STATUS_LABELS } from '../my-bookings/booking-view';

/**
 * A naptár és a beosztás bejegyzései FullCalendar-eseményként. A naptár az üzlet idejében
 * (Europe/Budapest) fut: a munkaidő helyi napként és órában érkezik ("2026-11-24", "09:00:00"),
 * a foglalás és a távollét UTC-időpontként; a FullCalendar mindkettőt budapesti időre teszi.
 */

/** Az eseményhez csatolt eredeti adat (extendedProps.entry). */
export type CalendarEntry =
  | { kind: 'booking'; booking: Booking }
  | { kind: 'availability'; availability: Availability }
  | { kind: 'timeOff'; timeOff: TimeOff };

/** Háttérként (a naptárban a munkaidő) vagy kattintható, húzható blokként jelenjen meg. */
export type EntryDisplay = 'block' | 'background';

/** A Material-téma színei; a FullCalendar CSS-változóként teszi az eseményre. */
const BOOKING_COLORS: Readonly<Record<BookingStatus, [string, string]>> = {
  BOOKED: ['var(--mat-sys-primary)', 'var(--mat-sys-on-primary)'],
  COMPLETED: ['var(--mat-sys-secondary)', 'var(--mat-sys-on-secondary)'],
  NO_SHOW: ['var(--mat-sys-error)', 'var(--mat-sys-on-error)'],
  CANCELLED: ['var(--mat-sys-outline)', 'var(--mat-sys-surface)'],
};

/**
 * A beosztásban a munkaidő-blokk áttetsző, hogy alatta látsszanak a foglalások (háttéresemények
 * a blokkok alatti rétegben).
 */
const AVAILABILITY_BLOCK_COLOR = 'color-mix(in oklab, var(--mat-sys-primary) 14%, transparent)';
const BOOKING_BACKGROUND_COLOR = 'var(--mat-sys-on-surface-variant)';

/** "09:00:00" vagy "09:00" → "09:00" */
export function hhmm(time: string): string {
  return time.slice(0, 5);
}

export function bookingEvent(booking: Booking, display: EntryDisplay = 'block'): EventInput {
  const [color, contrastColor] =
    display === 'background' ? [BOOKING_BACKGROUND_COLOR, ''] : BOOKING_COLORS[booking.status];
  const status = booking.status === 'BOOKED' ? '' : ` (${STATUS_LABELS[booking.status]})`;
  return {
    id: `booking-${booking.id}`,
    title: `${booking.customerName} · ${booking.serviceName}${status}`,
    start: booking.startAt,
    end: booking.endAt,
    display: display === 'background' ? 'background' : 'auto',
    color,
    contrastColor,
    editable: false,
    extendedProps: { entry: { kind: 'booking', booking } satisfies CalendarEntry },
  };
}

/**
 * Munkaidő-sáv. Blokként csak a mai és a későbbi napoké húzható: múltbeli sávot a backend
 * sem enged módosítani.
 */
export function availabilityEvent(
  availability: Availability,
  display: EntryDisplay,
  today: string,
): EventInput {
  return {
    id: `availability-${availability.id}`,
    title: 'Munkaidő',
    start: `${availability.workDate}T${hhmm(availability.startTime)}`,
    end: `${availability.workDate}T${hhmm(availability.endTime)}`,
    display: display === 'background' ? 'background' : 'auto',
    color: display === 'background' ? 'var(--mat-sys-primary)' : AVAILABILITY_BLOCK_COLOR,
    contrastColor: 'var(--mat-sys-on-primary-container)',
    editable: display === 'block' && availability.workDate >= today,
    extendedProps: { entry: { kind: 'availability', availability } satisfies CalendarEntry },
  };
}

export function timeOffEvent(timeOff: TimeOff, display: EntryDisplay): EventInput {
  return {
    id: `time-off-${timeOff.id}`,
    title: timeOff.reason ? `Távollét: ${timeOff.reason}` : 'Távollét',
    start: timeOff.startAt,
    end: timeOff.endAt,
    display: display === 'background' ? 'background' : 'auto',
    color: display === 'background' ? 'var(--mat-sys-error)' : 'var(--mat-sys-error-container)',
    contrastColor: 'var(--mat-sys-on-error-container)',
    editable: false,
    extendedProps: { entry: { kind: 'timeOff', timeOff } satisfies CalendarEntry },
  };
}

/** Az esemény eredeti adata; idegen eseménynél null. */
export function entryOf(event: { extendedProps: Record<string, unknown> }): CalendarEntry | null {
  return (event.extendedProps['entry'] as CalendarEntry | undefined) ?? null;
}

/** Egy időpont a naptár (az üzlet) idejében: nap és óra:perc. */
export interface WallClock {
  date: string;
  time: string;
}

const WALL_CLOCK = /^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2})/;
const HAS_OFFSET = /(Z|[+-]\d{2}:\d{2})$/;

/** A FullCalendar időpontszövege ("2026-11-24T09:00:00+01:00") → { date, time }. */
export function wallClock(value: string): WallClock {
  const match = WALL_CLOCK.exec(value);
  if (!match) {
    throw new Error(`Nem értelmezhető időpont: ${value}`);
  }
  return { date: match[1], time: match[2] };
}

/** Időzónával ellátott időpont a backendnek (a távollét OffsetDateTime-ot vár). */
export function withShopOffset(value: string): string {
  if (HAS_OFFSET.test(value)) {
    return value;
  }
  const { date, time } = wallClock(value);
  return `${date}T${time}:00${shopOffset(date, time)}`;
}

/** Az időpont ezredmásodpercben, a Date.now()-val összevethetően. */
export function instantOf(value: string): number {
  return Date.parse(withShopOffset(value));
}

/**
 * Kijelölt időszakból munkaidő-sáv; null, ha nem egy napon belül van (éjfélig tartó
 * kijelölés vége már a következő nap, azt sem lehet munkaidőnek felvenni).
 */
export function availabilityRequest(
  start: WallClock,
  end: WallClock,
): AvailabilitySlotRequest | null {
  if (start.date !== end.date) {
    return null;
  }
  return { workDate: start.date, startTime: start.time, endTime: end.time };
}

/** A nap hetének hétfője ("2026-11-26" → "2026-11-23"). */
export function mondayOf(date: string): string {
  const day = fromIsoDate(date);
  return toIsoDate(addDays(day, -((day.getDay() + 6) % 7)));
}

/** Egy ISO nap eltolva ("2026-11-23", -7 → "2026-11-16"). */
export function shiftDate(date: string, days: number): string {
  return toIsoDate(addDays(fromIsoDate(date), days));
}

const dayFormat = new Intl.DateTimeFormat('hu-HU', {
  timeZone: 'UTC',
  month: 'long',
  day: 'numeric',
  weekday: 'long',
});

/** "november 24., kedd" */
export function dayLabel(date: string): string {
  return dayFormat.format(new Date(`${date}T00:00:00Z`));
}

const shortDayFormat = new Intl.DateTimeFormat('hu-HU', {
  timeZone: 'UTC',
  month: 'short',
  day: 'numeric',
});

/** A hétfőtől vasárnapig tartó hét: "nov. 23. – nov. 29." */
export function weekLabel(monday: string): string {
  const day = (date: string) => shortDayFormat.format(new Date(`${date}T00:00:00Z`));
  return `${day(monday)} – ${day(shiftDate(monday, 6))}`;
}

/** "november 24., kedd, 09:00–12:00", több napon át: "november 24., kedd 12:00 – november 26., csütörtök 18:00" */
export function rangeLabel(start: WallClock, end: WallClock): string {
  return start.date === end.date
    ? `${dayLabel(start.date)}, ${start.time}–${end.time}`
    : `${dayLabel(start.date)} ${start.time} – ${dayLabel(end.date)} ${end.time}`;
}

/** Mit tehet a személyzet egy foglalással: kezdés előtt lemondhatja, utána lezárhatja. */
export type BookingAction = 'cancel' | 'complete' | 'noShow';

export function staffActions(booking: Booking, now: number): BookingAction[] {
  if (booking.status !== 'BOOKED') {
    return [];
  }
  return Date.parse(booking.startAt) > now ? ['cancel'] : ['complete', 'noShow'];
}
