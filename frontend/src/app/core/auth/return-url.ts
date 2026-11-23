import { User } from '../api/api.models';

/** Az oldalak, ahová belépés után nincs értelme visszairányítani. */
const AUTH_PAGES = ['/belepes', '/regisztracio'];

/**
 * Csak az alkalmazáson belüli, abszolút útvonal fogadható el visszatérési címként, különben
 * egy hamisított hivatkozás (?returnUrl=//gonosz.example) belépés után idegen oldalra vinne.
 */
export function isSafeReturnUrl(url: string | null | undefined): url is string {
  if (!url || !url.startsWith('/') || url.startsWith('//') || url.startsWith('/\\')) {
    return false;
  }
  const path = url.split(/[?#]/)[0];
  return !AUTH_PAGES.some((page) => path === page || path.startsWith(`${page}/`));
}

/** Hová menjen a felhasználó belépés vagy regisztráció után. */
export function afterLoginUrl(
  user: User,
  returnUrl: string | null | undefined,
  homeUrl: string,
): string {
  if (user.mustChangePassword) {
    return '/jelszo';
  }
  return isSafeReturnUrl(returnUrl) ? returnUrl : homeUrl;
}
