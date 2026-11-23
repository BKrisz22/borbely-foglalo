import { DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { DestroyRef, Injectable, InjectionToken, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';

import {
  AuthResponse,
  ChangePasswordRequest,
  LoginRequest,
  RegisterRequest,
  Role,
  User,
} from '../api/api.models';
import { Notifier } from '../ui/notifier';
import { isSafeReturnUrl } from './return-url';

/** Ahol a munkamenet a böngészőben megmarad; tesztben kicserélhető. */
export const AUTH_STORAGE = new InjectionToken<Storage>('AUTH_STORAGE', {
  providedIn: 'root',
  factory: () => localStorage,
});

export const SESSION_KEY = 'borbely.session';

/** A lejárat előtt ennyivel már lejártnak tekintjük a tokent (óraeltérés, hálózati késés). */
const EXPIRY_MARGIN_MS = 30_000;
/** A setTimeout legnagyobb késleltetése (kb. 24,8 nap). */
const MAX_TIMER_MS = 2_147_483_647;

interface Session {
  token: string;
  expiresAt: string;
  user: User;
}

/**
 * A bejelentkezett felhasználó állapota. A backend állapotmentes: belépéskor 1 órás JWT-t ad,
 * frissítőtoken nincs. A tokent a localStorage őrzi, így oldalfrissítés és új lap után is
 * megmarad; a lejáratkor (vagy ha a szerver 401-gyel utasít el egy tokenes kérést) a
 * munkamenet megszűnik, és a felhasználó a belépőoldalra kerül.
 *
 * Az állapot signalokban van, így a felület (zóna nélküli változásérzékelés mellett is)
 * magától frissül belépéskor és kilépéskor.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly storage = inject(AUTH_STORAGE);
  private readonly notifier = inject(Notifier);

  private readonly session = signal<Session | null>(this.restore());
  private expiryTimer: ReturnType<typeof setTimeout> | undefined;

  readonly user = computed(() => this.session()?.user ?? null);
  readonly role = computed<Role | null>(() => this.user()?.role ?? null);
  readonly isLoggedIn = computed(() => this.session() !== null);

  constructor() {
    this.scheduleExpiry();

    // Egy másik lapon történt be- vagy kilépés.
    const view = inject(DOCUMENT).defaultView;
    const onStorage = (event: StorageEvent) => {
      if (event.key === SESSION_KEY) {
        this.reloadFromStorage();
      }
    };
    view?.addEventListener('storage', onStorage);

    inject(DestroyRef).onDestroy(() => {
      clearTimeout(this.expiryTimer);
      view?.removeEventListener('storage', onStorage);
    });
  }

  /** Az érvényes token, vagy null, ha nincs belépve (vagy a token lejárt). */
  token(): string | null {
    const session = this.session();
    return session && !isExpired(session) ? session.token : null;
  }

  login(request: LoginRequest): Observable<User> {
    return this.http
      .post<AuthResponse>('/api/auth/login', request)
      .pipe(map((response) => this.start(response)));
  }

  /** A regisztráció rögtön be is léptet (a backend tokent ad vissza). */
  register(request: RegisterRequest): Observable<User> {
    return this.http
      .post<AuthResponse>('/api/auth/register', request)
      .pipe(map((response) => this.start(response)));
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http
      .put<void>('/api/auth/password', request)
      .pipe(tap(() => this.patchUser({ mustChangePassword: false })));
  }

  /**
   * Induláskor frissíti a tárolt felhasználói adatokat a szerverről (név, szerepkör,
   * borbély-azonosító). Nem várakoztat: ha a token már érvénytelen, az interceptor léptet ki.
   */
  syncUser(): void {
    if (!this.token()) {
      return;
    }
    this.http.get<User>('/api/auth/me').subscribe({
      next: (user) => this.patchUser(user),
      error: () => {
        // A 401-et az interceptor kezeli; más hibánál marad a tárolt adat.
      },
    });
  }

  logout(): void {
    this.clear();
    this.notifier.info('Kijelentkeztél.');
    void this.router.navigateByUrl('/');
  }

  /** A token lejárt vagy a szerver nem fogadja el: kiléptetés, majd vissza a belépéshez. */
  sessionExpired(): void {
    if (!this.session()) {
      return;
    }
    this.clear();
    this.notifier.error('A munkameneted lejárt, jelentkezz be újra.');
    const current = this.router.url;
    void this.router.navigate(['/belepes'], {
      queryParams: isSafeReturnUrl(current) ? { returnUrl: current } : {},
    });
  }

  /** A szerepkör kezdőoldala: a vendégé a foglalásai, a személyzeté a naptár. */
  homeUrl(): string {
    switch (this.role()) {
      case 'CUSTOMER':
        return '/foglalasaim';
      case 'BARBER':
      case 'OWNER':
        return '/naptar';
      default:
        return '/';
    }
  }

  private start(response: AuthResponse): User {
    this.persist({ token: response.token, expiresAt: response.expiresAt, user: response.user });
    return response.user;
  }

  private patchUser(changes: Partial<User>): void {
    const session = this.session();
    if (session) {
      this.persist({ ...session, user: { ...session.user, ...changes } });
    }
  }

  private persist(session: Session): void {
    this.session.set(session);
    try {
      this.storage.setItem(SESSION_KEY, JSON.stringify(session));
    } catch {
      // Tiltott vagy betelt tároló: a munkamenet csak ebben a lapban él.
    }
    this.scheduleExpiry();
  }

  private clear(): void {
    this.session.set(null);
    try {
      this.storage.removeItem(SESSION_KEY);
    } catch {
      // Lásd persist().
    }
    this.scheduleExpiry();
  }

  private restore(): Session | null {
    try {
      const raw = this.storage.getItem(SESSION_KEY);
      if (!raw) {
        return null;
      }
      const session = JSON.parse(raw) as Partial<Session> | null;
      if (!session?.token || !session.user || !session.expiresAt || isExpired(session as Session)) {
        this.storage.removeItem(SESSION_KEY);
        return null;
      }
      return session as Session;
    } catch {
      return null;
    }
  }

  private reloadFromStorage(): void {
    const previous = this.user();
    this.session.set(this.restore());
    this.scheduleExpiry();
    const current = this.user();
    if (previous && !current) {
      void this.router.navigateByUrl('/');
    } else if (current && current.id !== previous?.id) {
      void this.router.navigateByUrl(this.homeUrl());
    }
  }

  private scheduleExpiry(): void {
    clearTimeout(this.expiryTimer);
    this.expiryTimer = undefined;
    const session = this.session();
    if (!session) {
      return;
    }
    const delay = expiresAtMs(session) - EXPIRY_MARGIN_MS - Date.now();
    this.expiryTimer = setTimeout(
      () => this.sessionExpired(),
      Math.min(Math.max(delay, 0), MAX_TIMER_MS),
    );
  }
}

function expiresAtMs(session: Session): number {
  const value: unknown = session.expiresAt;
  if (typeof value === 'number') {
    // Ha a szerver epoch-másodpercként küldené.
    return value * 1000;
  }
  return typeof value === 'string' ? Date.parse(value) : Number.NaN;
}

function isExpired(session: Session): boolean {
  const expiresAt = expiresAtMs(session);
  return Number.isNaN(expiresAt) || expiresAt - EXPIRY_MARGIN_MS <= Date.now();
}
