import { Booking, BookingStatus } from '../../core/api/api.models';
import { canCustomerCancel, splitBookings } from './booking-view';

const NOW = Date.parse('2026-11-23T10:00:00Z');

function booking(id: number, startAt: string, status: BookingStatus = 'BOOKED'): Booking {
  return {
    id,
    barberId: 1,
    barberName: 'Kovács Péter',
    customerId: 7,
    customerName: 'Teszt Vilma',
    serviceId: 1,
    serviceName: 'Hajvágás',
    startAt,
    endAt: startAt,
    priceHuf: 6500,
    status,
    cancelledAt: null,
  };
}

describe('splitBookings', () => {
  it('a jövőbeli élő foglalások közelgők (legközelebbi elöl), a többi korábbi (legutóbbi elöl)', () => {
    const { upcoming, past } = splitBookings(
      [
        booking(1, '2026-11-30T09:00:00Z'),
        booking(2, '2026-11-25T09:00:00Z'),
        booking(3, '2026-11-20T09:00:00Z', 'COMPLETED'),
        booking(4, '2026-11-28T09:00:00Z', 'CANCELLED'),
        booking(5, '2026-11-22T09:00:00Z'),
      ],
      NOW,
    );
    expect(upcoming.map((b) => b.id)).toEqual([2, 1]);
    expect(past.map((b) => b.id)).toEqual([4, 5, 3]);
  });
});

describe('canCustomerCancel', () => {
  it('24 óránál korábban lemondható', () => {
    expect(canCustomerCancel(booking(1, '2026-11-24T10:00:00Z'), NOW)).toBe(true);
    expect(canCustomerCancel(booking(1, '2026-11-25T10:00:00Z'), NOW)).toBe(true);
  });

  it('24 órán belül már nem', () => {
    expect(canCustomerCancel(booking(1, '2026-11-24T09:59:00Z'), NOW)).toBe(false);
  });

  it('csak élő foglalás mondható le', () => {
    expect(canCustomerCancel(booking(1, '2026-11-30T10:00:00Z', 'CANCELLED'), NOW)).toBe(false);
  });
});
