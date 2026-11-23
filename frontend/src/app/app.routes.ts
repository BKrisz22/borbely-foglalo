import { CanActivateFn, Route, Routes } from '@angular/router';

import { authGuard, guestGuard, roleGuard } from './core/auth/auth.guards';
import { Shell } from './layout/shell';

/**
 * Egy még el nem készült oldal: a helyén leírás áll arról, mit fog tudni. A bemutatón így
 * a menü már a végleges szerkezetet mutatja. Az extra = true a bemutató utáni bővítéseket jelöli.
 */
function comingSoon(
  path: string,
  title: string,
  icon: string,
  plans: string[],
  canActivate: CanActivateFn[] = [],
  extra = false,
): Route {
  return {
    path,
    title,
    canActivate,
    data: { heading: title, icon, plans, extra },
    loadComponent: () =>
      import('./features/coming-soon/coming-soon-page').then((m) => m.ComingSoonPage),
  };
}

export const routes: Routes = [
  {
    path: '',
    component: Shell,
    children: [
      {
        path: '',
        title: 'Kezdőlap',
        loadComponent: () => import('./features/home/home-page').then((m) => m.HomePage),
      },
      {
        path: 'belepes',
        title: 'Bejelentkezés',
        canActivate: [guestGuard],
        loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
      },
      {
        path: 'regisztracio',
        title: 'Regisztráció',
        canActivate: [guestGuard],
        loadComponent: () => import('./features/auth/register-page').then((m) => m.RegisterPage),
      },
      {
        path: 'jelszo',
        title: 'Jelszó módosítása',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/auth/change-password-page').then((m) => m.ChangePasswordPage),
      },
      {
        // Belépés nélkül is végigjárható; a foglaláshoz a tölcsér végén kell belépni.
        path: 'foglalas',
        title: 'Időpontfoglalás',
        loadComponent: () => import('./features/booking/booking-page').then((m) => m.BookingPage),
      },
      {
        path: 'foglalasaim',
        title: 'Foglalásaim',
        canActivate: [roleGuard('CUSTOMER')],
        loadComponent: () =>
          import('./features/my-bookings/my-bookings-page').then((m) => m.MyBookingsPage),
      },
      {
        path: 'naptar',
        title: 'Naptár',
        canActivate: [roleGuard('BARBER', 'OWNER')],
        loadComponent: () => import('./features/staff/calendar-page').then((m) => m.CalendarPage),
      },
      {
        path: 'beosztas',
        title: 'Beosztás',
        canActivate: [roleGuard('BARBER', 'OWNER')],
        loadComponent: () => import('./features/staff/schedule-page').then((m) => m.SchedulePage),
      },
      comingSoon(
        'profil',
        'Profilom',
        'badge',
        [
          'Bemutatkozás szerkesztése (ez látszik a foglalásnál)',
          'Foglalási határidő: legalább hány perccel a kezdés előtt lehet foglalni',
        ],
        [roleGuard('BARBER')],
      ),
      comingSoon(
        'szolgaltatasok',
        'Szolgáltatások',
        'content_cut',
        [
          'Szolgáltatások felvétele és módosítása (név, időtartam, ár)',
          'Kivezetés: a korábbi foglalások megmaradnak',
        ],
        [roleGuard('OWNER')],
      ),
      comingSoon(
        'borbelyok',
        'Borbélyok',
        'groups',
        [
          'Borbély felvétele kezdőjelszóval; első belépéskor kötelező a jelszócsere',
          'A vállalt szolgáltatások beállítása',
          'Kikapcsolás és visszakapcsolás',
        ],
        [roleGuard('OWNER')],
      ),
      comingSoon(
        'penzugyek',
        'Pénzügyek',
        'payments',
        [
          'Bevétel borbélyonként és szolgáltatásonként, időszakra szűrve',
          'Kihasználtság és a meg nem jelent vendégek aránya',
          'E-számla kiállítása a lezárt foglalásokról (Számlázz.hu, teszt üzemmód)',
        ],
        [roleGuard('OWNER')],
        true,
      ),
      {
        path: '**',
        title: 'Nem található',
        loadComponent: () =>
          import('./features/not-found/not-found-page').then((m) => m.NotFoundPage),
      },
    ],
  },
];
