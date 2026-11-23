import { leadTimeLabel, leadTimeOptions } from './lead-time';

describe('leadTimeLabel', () => {
  it('percben, órában és napban is olvasható', () => {
    expect(leadTimeLabel(0)).toBe('Nincs határidő');
    expect(leadTimeLabel(30)).toBe('30 perc');
    expect(leadTimeLabel(60)).toBe('1 óra');
    expect(leadTimeLabel(90)).toBe('1 óra 30 perc');
    expect(leadTimeLabel(720)).toBe('12 óra');
    expect(leadTimeLabel(1440)).toBe('1 nap');
  });
});

describe('leadTimeOptions', () => {
  it('a mentett, de nem szokásos értéket is felkínálja, sorrendben', () => {
    const options = leadTimeOptions(45);
    expect(options).toContain(45);
    expect(options.indexOf(45)).toBe(options.indexOf(30) + 1);
  });

  it('a szokásos értéket nem duplázza', () => {
    expect(leadTimeOptions(60).filter((value) => value === 60)).toHaveLength(1);
  });
});
