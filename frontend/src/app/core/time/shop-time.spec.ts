import { shopDate, shopTime } from './shop-time';

describe('shop-time', () => {
  it('télen UTC+1 szerint írja ki az időt', () => {
    expect(shopTime('2026-11-26T09:00:00Z')).toBe('10:00');
  });

  it('nyáron UTC+2 szerint írja ki az időt', () => {
    expect(shopTime('2027-07-01T08:30:00Z')).toBe('10:30');
  });

  it('a napot is az üzlet ideje szerint adja (UTC szerint még előző nap)', () => {
    const date = shopDate('2026-11-25T23:30:00Z');
    expect(date).toContain('november 26.');
    expect(date).toContain('csütörtök');
  });
});
