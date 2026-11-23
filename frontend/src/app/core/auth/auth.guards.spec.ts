import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';

import { Role, User } from '../api/api.models';
import { Notifier } from '../ui/notifier';
import { authGuard, guestGuard, roleGuard } from './auth.guards';
import { AuthService } from './auth.service';

function user(role: Role, mustChangePassword = false): User {
  return {
    id: 1,
    email: 'x@example.com',
    fullName: 'Teszt Elek',
    role,
    mustChangePassword,
    barberId: null,
  };
}

describe('útvonalvédelem', () => {
  let notifier: { info: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };

  function setup(current: User | null, token: string | null = current ? 'token' : null): Router {
    const auth = {
      user: signal(current),
      token: () => token,
      homeUrl: () => (current?.role === 'CUSTOMER' ? '/foglalasaim' : current ? '/naptar' : '/'),
    };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: Notifier, useValue: notifier },
      ],
    });
    return TestBed.inject(Router);
  }

  function run(guard: CanActivateFn, url: string): unknown {
    return TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    );
  }

  function redirect(result: unknown): UrlTree {
    expect(result).toBeInstanceOf(UrlTree);
    return result as UrlTree;
  }

  beforeEach(() => {
    notifier = { info: vi.fn(), error: vi.fn() };
  });

  it('belépés nélkül a belépőoldalra visz, a visszatérési címmel', () => {
    const router = setup(null);
    const tree = redirect(run(authGuard, '/naptar'));

    expect(router.serializeUrl(tree).startsWith('/belepes')).toBe(true);
    expect(tree.queryParams['returnUrl']).toBe('/naptar');
  });

  it('lejárt tokennél is a belépőoldalra visz', () => {
    const router = setup(user('BARBER'), null);

    expect(
      router.serializeUrl(redirect(run(roleGuard('BARBER'), '/naptar'))).startsWith('/belepes'),
    ).toBe(true);
  });

  it('a megfelelő szerepkört átengedi', () => {
    setup(user('BARBER'));

    expect(run(roleGuard('BARBER', 'OWNER'), '/naptar')).toBe(true);
  });

  it('más szerepkört a saját kezdőoldalára visz, és szól', () => {
    const router = setup(user('CUSTOMER'));

    expect(router.serializeUrl(redirect(run(roleGuard('OWNER'), '/szolgaltatasok')))).toBe(
      '/foglalasaim',
    );
    expect(notifier.error).toHaveBeenCalled();
  });

  it('kötelező jelszócserénél mindenhonnan a jelszóoldalra visz', () => {
    const router = setup(user('BARBER', true));

    expect(router.serializeUrl(redirect(run(roleGuard('BARBER'), '/naptar')))).toBe('/jelszo');
    expect(run(authGuard, '/jelszo')).toBe(true);
  });

  it('a belépőoldal belépve a kezdőoldalra visz', () => {
    const router = setup(user('OWNER'));

    expect(router.serializeUrl(redirect(run(guestGuard, '/belepes')))).toBe('/naptar');
  });

  it('a belépőoldal kijelentkezve elérhető', () => {
    setup(null);

    expect(run(guestGuard, '/belepes')).toBe(true);
  });
});
