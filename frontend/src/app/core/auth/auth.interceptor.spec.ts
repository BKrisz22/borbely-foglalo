import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  function setup(token: string | null) {
    const auth = { token: () => token, sessionExpired: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
    return {
      auth,
      client: TestBed.inject(HttpClient),
      http: TestBed.inject(HttpTestingController),
    };
  }

  it('az API-kérésekhez hozzáteszi a Bearer tokent', () => {
    const { client, http } = setup('abc');
    client.get('/api/bookings').subscribe();

    expect(http.expectOne('/api/bookings').request.headers.get('Authorization')).toBe('Bearer abc');
  });

  it('más címre nem küldi el a tokent', () => {
    const { client, http } = setup('abc');
    client.get('https://fonts.googleapis.com/css2').subscribe();

    expect(
      http.expectOne('https://fonts.googleapis.com/css2').request.headers.has('Authorization'),
    ).toBe(false);
  });

  it('tokennel küldött kérésre kapott 401 esetén kiléptet', () => {
    const { auth, client, http } = setup('abc');
    client.get('/api/bookings').subscribe({ error: () => undefined });
    http.expectOne('/api/bookings').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(auth.sessionExpired).toHaveBeenCalledTimes(1);
  });

  it('token nélküli 401 (hibás jelszó) nem léptet ki', () => {
    const { auth, client, http } = setup(null);
    client.post('/api/auth/login', {}).subscribe({ error: () => undefined });
    http
      .expectOne('/api/auth/login')
      .flush({ title: 'Sikertelen bejelentkezés' }, { status: 401, statusText: 'Unauthorized' });

    expect(auth.sessionExpired).not.toHaveBeenCalled();
  });

  it('más hibánál (például 409) nem léptet ki', () => {
    const { auth, client, http } = setup('abc');
    client.post('/api/bookings', {}).subscribe({ error: () => undefined });
    http
      .expectOne('/api/bookings')
      .flush({ title: 'Ütközés' }, { status: 409, statusText: 'Conflict' });

    expect(auth.sessionExpired).not.toHaveBeenCalled();
  });
});
