import { convertToParamMap } from '@angular/router';

import { Slot } from '../../core/api/api.models';
import {
  addMinutesToTime,
  groupSlots,
  readPendingSelection,
  sameInstant,
} from './booking-selection';

const slot = (time: string): Slot => ({ startAt: `2026-12-01T${time}:00+01:00`, time });

describe('readPendingSelection', () => {
  it('kiolvassa a teljes választást', () => {
    const params = convertToParamMap({
      serviceId: '1',
      barberId: '2',
      date: '2026-12-01',
      startAt: '2026-12-01T08:00:00Z',
    });
    expect(readPendingSelection(params)).toEqual({
      serviceId: 1,
      barberId: 2,
      date: '2026-12-01',
      startAt: '2026-12-01T08:00:00Z',
    });
  });

  it('szolgáltatás és borbély nélkül nincs mit visszaállítani', () => {
    expect(readPendingSelection(convertToParamMap({}))).toBeNull();
    expect(readPendingSelection(convertToParamMap({ date: '2026-12-01' }))).toBeNull();
    expect(readPendingSelection(convertToParamMap({ serviceId: 'abc' }))).toBeNull();
    expect(readPendingSelection(convertToParamMap({ serviceId: '-3' }))).toBeNull();
  });

  it('a naptárból érkező előválasztásban még nincs szolgáltatás', () => {
    const params = convertToParamMap({
      barberId: '2',
      date: '2026-12-01',
      startAt: '2026-12-01T10:00:00+01:00',
    });
    expect(readPendingSelection(params)).toEqual({
      serviceId: null,
      barberId: 2,
      date: '2026-12-01',
      startAt: '2026-12-01T10:00:00+01:00',
    });
  });

  it('a hibás részeket elhagyja', () => {
    expect(
      readPendingSelection(
        convertToParamMap({ serviceId: '1', barberId: 'x', date: '2026-02-30' }),
      ),
    ).toEqual({
      serviceId: 1,
      barberId: null,
      date: null,
      startAt: null,
    });
  });
});

describe('groupSlots', () => {
  it('napszakok szerint csoportosít, az üres csoportokat elhagyja', () => {
    const groups = groupSlots([slot('09:00'), slot('11:45'), slot('12:00'), slot('16:45')]);
    expect(groups.map((group) => group.label)).toEqual(['Délelőtt', 'Délután']);
    expect(groups[0].slots.map((s) => s.time)).toEqual(['09:00', '11:45']);
    expect(groups[1].slots.map((s) => s.time)).toEqual(['12:00', '16:45']);
  });

  it('17 órától este', () => {
    expect(groupSlots([slot('17:00')])[0].label).toBe('Este');
  });

  it('üres listára üres eredmény', () => {
    expect(groupSlots([])).toEqual([]);
  });
});

describe('addMinutesToTime', () => {
  it('perceket ad egy helyi időhöz', () => {
    expect(addMinutesToTime('09:45', 30)).toBe('10:15');
    expect(addMinutesToTime('17:15', 45)).toBe('18:00');
    expect(addMinutesToTime('08:00', 20)).toBe('08:20');
  });
});

describe('sameInstant', () => {
  it('az időzóna-jelöléstől függetlenül hasonlít', () => {
    expect(sameInstant('2026-12-01T08:00:00Z', '2026-12-01T09:00:00+01:00')).toBe(true);
    expect(sameInstant('2026-12-01T08:00:00Z', '2026-12-01T08:15:00Z')).toBe(false);
    expect(sameInstant('nem dátum', 'nem dátum')).toBe(false);
  });
});
