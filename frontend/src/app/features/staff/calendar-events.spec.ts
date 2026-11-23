import { Availability, Booking, BookingStatus, TimeOff } from '../../core/api/api.models';
import {
  availabilityEvent,
  availabilityRequest,
  bookingEvent,
  entryOf,
  instantOf,
  mondayOf,
  rangeLabel,
  shiftDate,
  weekLabel,
  staffActions,
  timeOffEvent,
  wallClock,
  withShopOffset,
} from './calendar-events';

function booking(status: BookingStatus = 'BOOKED', startAt = '2026-11-24T09:00:00Z'): Booking {
  return {
    id: 12,
    barberId: 1,
    barberName: 'Kovács Péter',
    customerId: 7,
    customerName: 'Teszt Vilma',
    serviceId: 1,
    serviceName: 'Hajvágás',
    startAt,
    endAt: '2026-11-24T09:30:00Z',
    priceHuf: 6500,
    status,
    cancelledAt: null,
  };
}

const AVAILABILITY: Availability = {
  id: 5,
  barberId: 1,
  workDate: '2026-11-24',
  startTime: '09:00:00',
  endTime: '17:00:00',
};

const TIME_OFF: TimeOff = {
  id: 3,
  barberId: 1,
  startAt: '2026-11-25T11:00:00Z',
  endAt: '2026-11-25T12:00:00Z',
  reason: 'Orvos',
};

describe('bookingEvent', () => {
  it('a vendég és a szolgáltatás a cím, az eredeti foglalás az eseményen marad', () => {
    const event = bookingEvent(booking());
    expect(event.id).toBe('booking-12');
    expect(event.title).toBe('Teszt Vilma · Hajvágás');
    expect(event.start).toBe('2026-11-24T09:00:00Z');
    expect(event.display).toBe('auto');
    expect(event.editable).toBe(false);
    expect(entryOf({ extendedProps: event.extendedProps ?? {} })).toEqual({
      kind: 'booking',
      booking: booking(),
    });
  });

  it('a lezárt foglalás címe az állapotot is mutatja', () => {
    expect(bookingEvent(booking('NO_SHOW')).title).toBe('Teszt Vilma · Hajvágás (Nem jelent meg)');
  });

  it('háttérként szürke', () => {
    const event = bookingEvent(booking(), 'background');
    expect(event.display).toBe('background');
    expect(event.color).toBe('var(--mat-sys-on-surface-variant)');
  });
});

describe('availabilityEvent', () => {
  it('a helyi napot és órát időzóna nélkül adja át (a naptár az üzlet idejében fut)', () => {
    const event = availabilityEvent(AVAILABILITY, 'block', '2026-11-23');
    expect(event.start).toBe('2026-11-24T09:00');
    expect(event.end).toBe('2026-11-24T17:00');
    expect(event.editable).toBe(true);
  });

  it('múltbeli nap sávja nem húzható, háttérként sem', () => {
    expect(availabilityEvent(AVAILABILITY, 'block', '2026-11-25').editable).toBe(false);
    expect(availabilityEvent(AVAILABILITY, 'background', '2026-11-23').editable).toBe(false);
  });
});

describe('timeOffEvent', () => {
  it('az indoklás a címben', () => {
    expect(timeOffEvent(TIME_OFF, 'block').title).toBe('Távollét: Orvos');
    expect(timeOffEvent({ ...TIME_OFF, reason: null }, 'block').title).toBe('Távollét');
  });
});

describe('wallClock', () => {
  it('a FullCalendar szövegéből napot és órát vesz ki, időzónától függetlenül', () => {
    expect(wallClock('2026-11-24T09:15:00+01:00')).toEqual({ date: '2026-11-24', time: '09:15' });
    expect(wallClock('2026-11-24T09:15:00')).toEqual({ date: '2026-11-24', time: '09:15' });
  });

  it('értelmezhetetlen szövegre hibát dob', () => {
    expect(() => wallClock('2026-11-24')).toThrow();
  });
});

describe('withShopOffset', () => {
  it('meglévő eltérést megtart', () => {
    expect(withShopOffset('2026-11-24T09:00:00+01:00')).toBe('2026-11-24T09:00:00+01:00');
  });

  it('időzóna nélküli időpontot budapesti eltéréssel lát el (télen +1, nyáron +2 óra)', () => {
    expect(withShopOffset('2026-11-24T09:00:00')).toBe('2026-11-24T09:00:00+01:00');
    expect(withShopOffset('2027-07-01T09:00:00')).toBe('2027-07-01T09:00:00+02:00');
  });

  it('instantOf ugyanazt az időpontot adja', () => {
    expect(instantOf('2026-11-24T09:00:00')).toBe(Date.parse('2026-11-24T08:00:00Z'));
  });
});

describe('availabilityRequest', () => {
  it('egy napon belüli kijelölésből sáv lesz', () => {
    expect(
      availabilityRequest(
        { date: '2026-11-24', time: '09:00' },
        { date: '2026-11-24', time: '12:30' },
      ),
    ).toEqual({ workDate: '2026-11-24', startTime: '09:00', endTime: '12:30' });
  });

  it('napokon átnyúló kijelölésből nem', () => {
    expect(
      availabilityRequest(
        { date: '2026-11-24', time: '20:00' },
        { date: '2026-11-25', time: '00:00' },
      ),
    ).toBeNull();
  });
});

describe('hét és nap', () => {
  it('mondayOf a hét hétfőjét adja (vasárnap az előző hétfő)', () => {
    expect(mondayOf('2026-11-23')).toBe('2026-11-23');
    expect(mondayOf('2026-11-26')).toBe('2026-11-23');
    expect(mondayOf('2026-11-29')).toBe('2026-11-23');
  });

  it('shiftDate hónapon át is jól lép', () => {
    expect(shiftDate('2026-12-01', -7)).toBe('2026-11-24');
  });

  it('weekLabel a hétfőtől vasárnapig tartó hetet írja ki', () => {
    expect(weekLabel('2026-11-30')).toBe('nov. 30. – dec. 6.');
  });

  it('rangeLabel', () => {
    expect(
      rangeLabel({ date: '2026-11-24', time: '09:00' }, { date: '2026-11-24', time: '12:00' }),
    ).toBe('november 24., kedd, 09:00–12:00');
    expect(
      rangeLabel({ date: '2026-11-24', time: '12:00' }, { date: '2026-11-26', time: '18:00' }),
    ).toBe('november 24., kedd 12:00 – november 26., csütörtök 18:00');
  });
});

describe('staffActions', () => {
  const now = Date.parse('2026-11-24T10:00:00Z');

  it('kezdés előtt lemondható', () => {
    expect(staffActions(booking('BOOKED', '2026-11-24T11:00:00Z'), now)).toEqual(['cancel']);
  });

  it('kezdés után lezárható', () => {
    expect(staffActions(booking('BOOKED', '2026-11-24T09:00:00Z'), now)).toEqual([
      'complete',
      'noShow',
    ]);
  });

  it('lezárt vagy lemondott foglalással nincs teendő', () => {
    expect(staffActions(booking('COMPLETED'), now)).toEqual([]);
    expect(staffActions(booking('CANCELLED'), now)).toEqual([]);
  });
});
