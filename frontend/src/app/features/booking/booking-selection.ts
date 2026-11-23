import { ParamMap } from '@angular/router';

import { Slot } from '../../core/api/api.models';
import { isIsoDate } from '../../core/time/local-date';

/**
 * A címsorban megőrzött választás (?serviceId=1&barberId=2&date=2026-12-01&startAt=…).
 * Így a tölcsér belépés vagy oldalfrissítés után ugyanott folytatódik, és a hivatkozás
 * meg is osztható. Szolgáltatás nélkül (?barberId=2&date=…&startAt=…) a naptárból érkező
 * előválasztás: a borbély, a nap és az időpont a szolgáltatás kiválasztása után érvényesül.
 */
export interface PendingSelection {
  /** null: előválasztás a naptárból, a szolgáltatást még választani kell. */
  serviceId: number | null;
  barberId: number | null;
  date: string | null;
  startAt: string | null;
}

export function readPendingSelection(params: ParamMap): PendingSelection | null {
  const serviceId = toId(params.get('serviceId'));
  const barberId = toId(params.get('barberId'));
  // Szolgáltatás és borbély nélkül nincs mit visszaállítani.
  if (serviceId === null && barberId === null) {
    return null;
  }
  const date = params.get('date');
  return {
    serviceId,
    barberId,
    date: isIsoDate(date) ? date : null,
    startAt: params.get('startAt'),
  };
}

function toId(value: string | null): number | null {
  const id = Number(value);
  return value !== null && Number.isInteger(id) && id > 0 ? id : null;
}

export interface SlotGroup {
  label: string;
  slots: Slot[];
}

/** Napszakok szerint csoportosít (a slot helyi ideje alapján): délelőtt, délután (12-től), este (17-től). */
export function groupSlots(slots: readonly Slot[]): SlotGroup[] {
  const groups: SlotGroup[] = [
    { label: 'Délelőtt', slots: [] },
    { label: 'Délután', slots: [] },
    { label: 'Este', slots: [] },
  ];
  for (const slot of slots) {
    const hour = Number(slot.time.slice(0, 2));
    groups[hour < 12 ? 0 : hour < 17 ? 1 : 2].slots.push(slot);
  }
  return groups.filter((group) => group.slots.length > 0);
}

/** "09:45" + 30 perc → "10:15". */
export function addMinutesToTime(time: string, minutes: number): string {
  const [hours, mins] = time.split(':').map(Number);
  const total = (hours * 60 + mins + minutes) % (24 * 60);
  const pad = (value: number) => value.toString().padStart(2, '0');
  return `${pad(Math.floor(total / 60))}:${pad(total % 60)}`;
}

/** Ugyanazt az időpillanatot jelöli-e a két ISO időpont (például "…T08:00:00Z" és "…T09:00:00+01:00"). */
export function sameInstant(a: string, b: string): boolean {
  const first = Date.parse(a);
  return !Number.isNaN(first) && first === Date.parse(b);
}
