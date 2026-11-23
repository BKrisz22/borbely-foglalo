import { addDays, fromIsoDate, isIsoDate, startOfDay, toIsoDate } from './local-date';

describe('local-date', () => {
  it('oda-vissza alakít a "ÉÉÉÉ-HH-NN" forma és a helyi éjfél között', () => {
    const date = fromIsoDate('2026-12-01');
    expect(date.getFullYear()).toBe(2026);
    expect(date.getMonth()).toBe(11);
    expect(date.getDate()).toBe(1);
    expect(date.getHours()).toBe(0);
    expect(toIsoDate(date)).toBe('2026-12-01');
  });

  it('a napokat hónap- és évhatáron át is helyesen lépteti', () => {
    expect(toIsoDate(addDays(fromIsoDate('2026-12-30'), 3))).toBe('2027-01-02');
    // Óraátállítás hetében is pontosan egy naptári nap a lépés.
    expect(toIsoDate(addDays(fromIsoDate('2027-03-27'), 1))).toBe('2027-03-28');
    expect(toIsoDate(addDays(fromIsoDate('2027-03-28'), 1))).toBe('2027-03-29');
  });

  it('a nap kezdetére kerekít', () => {
    const start = startOfDay(new Date(2026, 10, 23, 17, 45));
    expect(toIsoDate(start)).toBe('2026-11-23');
    expect(start.getHours()).toBe(0);
    expect(start.getMinutes()).toBe(0);
  });

  it('csak érvényes dátumot fogad el', () => {
    expect(isIsoDate('2026-12-01')).toBe(true);
    expect(isIsoDate('2026-02-30')).toBe(false);
    expect(isIsoDate('2026-1-5')).toBe(false);
    expect(isIsoDate('holnap')).toBe(false);
    expect(isIsoDate(null)).toBe(false);
  });
});
