import { User } from '../api/api.models';
import { afterLoginUrl, isSafeReturnUrl } from './return-url';

const USER: User = {
  id: 1,
  email: 'vilma@example.com',
  fullName: 'Teszt Vilma',
  role: 'CUSTOMER',
  mustChangePassword: false,
  barberId: null,
};

describe('isSafeReturnUrl', () => {
  it('az alkalmazáson belüli útvonalat elfogadja', () => {
    expect(isSafeReturnUrl('/')).toBe(true);
    expect(isSafeReturnUrl('/foglalas?serviceId=2')).toBe(true);
  });

  it('külső címre nem enged visszairányítani', () => {
    expect(isSafeReturnUrl('https://gonosz.example')).toBe(false);
    expect(isSafeReturnUrl('//gonosz.example')).toBe(false);
    expect(isSafeReturnUrl('/\\gonosz.example')).toBe(false);
  });

  it('a belépő- és regisztrációs oldalra nem irányít vissza', () => {
    expect(isSafeReturnUrl('/belepes')).toBe(false);
    expect(isSafeReturnUrl('/regisztracio?returnUrl=%2F')).toBe(false);
  });

  it('hiányzó értéknél hamis', () => {
    expect(isSafeReturnUrl(undefined)).toBe(false);
    expect(isSafeReturnUrl('')).toBe(false);
  });
});

describe('afterLoginUrl', () => {
  it('kötelező jelszócserénél a jelszóoldalra visz', () => {
    expect(afterLoginUrl({ ...USER, mustChangePassword: true }, '/foglalas', '/foglalasaim')).toBe(
      '/jelszo',
    );
  });

  it('a biztonságos visszatérési címre visz', () => {
    expect(afterLoginUrl(USER, '/foglalas', '/foglalasaim')).toBe('/foglalas');
  });

  it('különben a szerepkör kezdőoldalára', () => {
    expect(afterLoginUrl(USER, undefined, '/foglalasaim')).toBe('/foglalasaim');
    expect(afterLoginUrl(USER, '//gonosz.example', '/foglalasaim')).toBe('/foglalasaim');
  });
});
