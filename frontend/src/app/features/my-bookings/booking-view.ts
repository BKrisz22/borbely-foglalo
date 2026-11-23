import { Booking, BookingStatus } from '../../core/api/api.models';

/** Hány órával a kezdés előtt mondhatja le a vendég maga (backend: app.booking.cancel-deadline). */
export const CANCEL_DEADLINE_HOURS = 24;

export const STATUS_LABELS: Readonly<Record<BookingStatus, string>> = {
  BOOKED: 'Foglalt',
  COMPLETED: 'Teljesítve',
  CANCELLED: 'Lemondva',
  NO_SHOW: 'Nem jelent meg',
};

export interface SplitBookings {
  /** Még el nem kezdődött, élő foglalások, a legközelebbi elöl. */
  upcoming: Booking[];
  /** Minden más (elmúlt, lemondott, lezárt), a legutóbbi elöl. */
  past: Booking[];
}

export function splitBookings(bookings: readonly Booking[], now: number): SplitBookings {
  const upcoming: Booking[] = [];
  const past: Booking[] = [];
  for (const booking of bookings) {
    if (booking.status === 'BOOKED' && Date.parse(booking.startAt) > now) {
      upcoming.push(booking);
    } else {
      past.push(booking);
    }
  }
  upcoming.sort((a, b) => Date.parse(a.startAt) - Date.parse(b.startAt));
  past.sort((a, b) => Date.parse(b.startAt) - Date.parse(a.startAt));
  return { upcoming, past };
}

/** A vendég akkor mondhatja le maga, ha a kezdésig még legalább 24 óra van (a backend is így dönt). */
export function canCustomerCancel(booking: Booking, now: number): boolean {
  if (booking.status !== 'BOOKED') {
    return false;
  }
  return Date.parse(booking.startAt) - CANCEL_DEADLINE_HOURS * 3_600_000 >= now;
}
