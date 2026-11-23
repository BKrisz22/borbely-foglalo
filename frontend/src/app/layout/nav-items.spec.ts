import { navItemsFor } from './nav-items';

const paths = (role: Parameters<typeof navItemsFor>[0]) =>
  navItemsFor(role).map((item) => item.path);

describe('navItemsFor', () => {
  it('kijelentkezve csak a kezdőlap és a foglalás látszik', () => {
    expect(paths(null)).toEqual(['/', '/foglalas']);
  });

  it('a vendég a saját foglalásait is látja', () => {
    expect(paths('CUSTOMER')).toEqual(['/', '/foglalas', '/foglalasaim']);
  });

  it('a borbély a naptárt, a beosztást és a profilját látja, a tulajdonosi oldalakat nem', () => {
    const items = paths('BARBER');
    expect(items).toEqual(['/', '/naptar', '/foglalas', '/beosztas', '/profil']);
    expect(items).not.toContain('/szolgaltatasok');
  });

  it('a tulajdonos kezeli a szolgáltatásokat és a borbélyokat; a pénzügyek még csak terv', () => {
    const items = navItemsFor('OWNER');
    expect(items.map((item) => item.path)).toEqual([
      '/',
      '/naptar',
      '/foglalas',
      '/beosztas',
      '/szolgaltatasok',
      '/borbelyok',
      '/penzugyek',
    ]);
    expect(items.find((item) => item.path === '/penzugyek')?.soon).toBe(true);
  });

  it('a kezdőlap csak pontos egyezésnél kiemelt', () => {
    expect(navItemsFor(null)[0].exact).toBe(true);
  });
});
