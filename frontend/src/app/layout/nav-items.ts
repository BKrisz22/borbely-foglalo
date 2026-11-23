import { Role } from '../core/api/api.models';

export interface NavItem {
  label: string;
  icon: string;
  path: string;
  /** Csak pontos egyezésnél legyen kiemelve (a kezdőlapnál kell, mert minden útvonal '/'-rel kezdődik). */
  exact?: boolean;
  /** A bemutató utáni bővítések része; a menüben „hamarosan” jelölést kap. */
  soon?: boolean;
}

export const ROLE_LABELS: Readonly<Record<Role, string>> = {
  CUSTOMER: 'Vendég',
  BARBER: 'Borbély',
  OWNER: 'Tulajdonos',
};

const HOME: NavItem = { label: 'Kezdőlap', icon: 'home', path: '/', exact: true };
const BOOKING: NavItem = { label: 'Időpontfoglalás', icon: 'event_available', path: '/foglalas' };
const CALENDAR: NavItem = { label: 'Naptár', icon: 'calendar_month', path: '/naptar' };
const SCHEDULE: NavItem = { label: 'Beosztás', icon: 'schedule', path: '/beosztas' };
/** A személyzetnek ugyanaz a tölcsér, vendég nevében (telefonos foglalás). */
const STAFF_BOOKING: NavItem = {
  label: 'Foglalás felvétele',
  icon: 'add_circle',
  path: '/foglalas',
};

/** A menü pontjai szerepkör szerint; null = nincs belépve. */
export function navItemsFor(role: Role | null): NavItem[] {
  switch (role) {
    case 'CUSTOMER':
      return [HOME, BOOKING, { label: 'Foglalásaim', icon: 'event_note', path: '/foglalasaim' }];
    case 'BARBER':
      return [
        HOME,
        CALENDAR,
        STAFF_BOOKING,
        SCHEDULE,
        { label: 'Profilom', icon: 'badge', path: '/profil' },
      ];
    case 'OWNER':
      return [
        HOME,
        CALENDAR,
        STAFF_BOOKING,
        SCHEDULE,
        { label: 'Szolgáltatások', icon: 'content_cut', path: '/szolgaltatasok' },
        { label: 'Borbélyok', icon: 'groups', path: '/borbelyok' },
        { label: 'Pénzügyek', icon: 'payments', path: '/penzugyek', soon: true },
      ];
    default:
      return [HOME, BOOKING];
  }
}
