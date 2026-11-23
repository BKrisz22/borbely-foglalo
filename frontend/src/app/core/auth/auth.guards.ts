import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

import { Role } from '../api/api.models';
import { Notifier } from '../ui/notifier';
import { AuthService } from './auth.service';

/**
 * Útvonalvédelem. A felület csak kényelmi szűrő: a jogosultságot minden kérésnél a backend
 * ellenőrzi (SecurityConfig, szolgáltatásszintű tulajdonos-ellenőrzés).
 */

/** Belépés kell; ha kötelező a jelszócsere, előbb az. */
export const authGuard: CanActivateFn = (_route, state) => checkAccess(state.url, null);

/** Belépés és a felsorolt szerepkörök egyike kell. */
export function roleGuard(...roles: Role[]): CanActivateFn {
  return (_route, state) => checkAccess(state.url, roles);
}

/** Csak kijelentkezve (belépés, regisztráció); belépve a szerepkör kezdőoldalára visz. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.token() ? inject(Router).parseUrl(auth.homeUrl()) : true;
};

function checkAccess(url: string, roles: readonly Role[] | null): boolean | UrlTree {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.user();
  if (!user || !auth.token()) {
    return router.createUrlTree(['/belepes'], { queryParams: { returnUrl: url } });
  }
  if (user.mustChangePassword && !url.startsWith('/jelszo')) {
    return router.createUrlTree(['/jelszo']);
  }
  if (roles && !roles.includes(user.role)) {
    inject(Notifier).error('Ehhez az oldalhoz nincs jogosultságod.');
    return router.parseUrl(auth.homeUrl());
  }
  return true;
}
