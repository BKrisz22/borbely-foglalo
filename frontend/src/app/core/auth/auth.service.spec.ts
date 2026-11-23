import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthResponse, User } from '../api/api.models';
import { Notifier } from '../ui/notifier';
import { AUTH_STORAGE, AuthService, SESSION_KEY } from './auth.service';

const CUSTOMER: User = {
  id: 7,
  email: 'vilma@example.com',
  fullName: 'Teszt Vilma',
  role: 'CUSTOMER',
  mustChangePassword: false,
  barberId: null,
};

const BARBER: User = {
  id: 3,
  email: 'kovacs.peter@example.com',
  fullName: 'Kovács Péter',
  role: 'BARBER',
  mustChangePassword: false,
  barberId: 1,
};

function inMinutes(minutes: number): string {
  return new Date(Date.now() + minutes * 60_000).toISOString();
}

function authResponse(user: User, expiresAt = inMinutes(60), token = 'token-1'): AuthResponse {
  return { token, expiresAt, user };
}

/** Memóriában tárolt localStorage-pótló. */
function memoryStorage(): Storage {
  const data = new Map<string, string>();
  return {
    get length() {
      return data.size;
    },
    clear: () => data.clear(),
    getItem: (key) => data.get(key) ?? null,
    key: (index) => [...data.keys()][index] ?? null,
    removeItem: (key) => {
      data.delete(key);
    },
    setItem: (key, value) => {
      data.set(key, value);
    },
  };
}

describe('AuthService', () => {
  let storage: Storage;
  let notifier: { info: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };

  function setup(): { auth: AuthService; http: HttpTestingController } {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        // Bármely címre lehessen navigálni (kilépés, belépőoldal).
        provideRouter([{ path: '**', children: [] }]),
        { provide: AUTH_STORAGE, useValue: storage },
        { provide: Notifier, useValue: notifier },
      ],
    });
    return { auth: TestBed.inject(AuthService), http: TestBed.inject(HttpTestingController) };
  }

  function storedSession(): { token: string; user: User } {
    return JSON.parse(storage.getItem(SESSION_KEY) ?? 'null') as { token: string; user: User };
  }

  beforeEach(() => {
    storage = memoryStorage();
    notifier = { info: vi.fn(), error: vi.fn() };
  });

  it('belépéskor eltárolja a munkamenetet, és onnantól a tokent adja', () => {
    const { auth, http } = setup();
    let loggedIn: User | undefined;
    auth
      .login({ email: 'vilma@example.com', password: 'Demo1234' })
      .subscribe((user) => (loggedIn = user));

    const request = http.expectOne('/api/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'vilma@example.com', password: 'Demo1234' });
    request.flush(authResponse(CUSTOMER));

    expect(loggedIn).toEqual(CUSTOMER);
    expect(auth.isLoggedIn()).toBe(true);
    expect(auth.token()).toBe('token-1');
    expect(auth.role()).toBe('CUSTOMER');
    expect(storedSession().token).toBe('token-1');
    http.verify();
  });

  it('induláskor visszaállítja a tárolt, érvényes munkamenetet', () => {
    storage.setItem(SESSION_KEY, JSON.stringify(authResponse(BARBER)));
    const { auth } = setup();

    expect(auth.isLoggedIn()).toBe(true);
    expect(auth.user()?.barberId).toBe(1);
  });

  it('a lejárt munkamenetet induláskor eldobja', () => {
    storage.setItem(SESSION_KEY, JSON.stringify(authResponse(CUSTOMER, inMinutes(-1))));
    const { auth } = setup();

    expect(auth.isLoggedIn()).toBe(false);
    expect(auth.token()).toBeNull();
    expect(storage.getItem(SESSION_KEY)).toBeNull();
  });

  it('a sérült tárolt adatot figyelmen kívül hagyja', () => {
    storage.setItem(SESSION_KEY, '{nem json');
    const { auth } = setup();

    expect(auth.isLoggedIn()).toBe(false);
  });

  it('a kezdőoldal szerepkörtől függ', () => {
    const { auth, http } = setup();
    expect(auth.homeUrl()).toBe('/');

    auth.login({ email: 'kovacs.peter@example.com', password: 'Demo1234' }).subscribe();
    http.expectOne('/api/auth/login').flush(authResponse(BARBER));
    expect(auth.homeUrl()).toBe('/naptar');
  });

  it('kijelentkezéskor törli a munkamenetet', () => {
    storage.setItem(SESSION_KEY, JSON.stringify(authResponse(CUSTOMER)));
    const { auth } = setup();

    auth.logout();

    expect(auth.isLoggedIn()).toBe(false);
    expect(storage.getItem(SESSION_KEY)).toBeNull();
    expect(notifier.info).toHaveBeenCalledWith('Kijelentkeztél.');
  });

  it('jelszócsere után leveszi a kötelező jelszócsere jelzőjét', () => {
    storage.setItem(
      SESSION_KEY,
      JSON.stringify(authResponse({ ...BARBER, mustChangePassword: true })),
    );
    const { auth, http } = setup();

    auth.changePassword({ currentPassword: 'Kezdo1234', newPassword: 'Sajat1234' }).subscribe();
    const request = http.expectOne('/api/auth/password');
    expect(request.request.method).toBe('PUT');
    request.flush(null, { status: 204, statusText: 'No Content' });

    expect(auth.user()?.mustChangePassword).toBe(false);
    expect(storedSession().user.mustChangePassword).toBe(false);
  });

  it('induláskor frissíti a felhasználó adatait a szerverről', () => {
    storage.setItem(SESSION_KEY, JSON.stringify(authResponse(BARBER)));
    const { auth, http } = setup();

    auth.syncUser();
    http.expectOne('/api/auth/me').flush({ ...BARBER, fullName: 'Kovács Péter Pál' });

    expect(auth.user()?.fullName).toBe('Kovács Péter Pál');
  });

  it('lejárt munkamenetnél kiléptet és szól', () => {
    storage.setItem(SESSION_KEY, JSON.stringify(authResponse(CUSTOMER)));
    const { auth } = setup();

    auth.sessionExpired();

    expect(auth.isLoggedIn()).toBe(false);
    expect(notifier.error).toHaveBeenCalledWith('A munkameneted lejárt, jelentkezz be újra.');
  });
});
