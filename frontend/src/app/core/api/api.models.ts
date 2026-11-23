/**
 * A backend REST API-jának adattípusai. A mezőnevek a Java rekordokéval egyeznek
 * (auth/dto, serviceoffering/dto, barber/dto, slot/dto, booking/dto, schedule/dto, customer/dto).
 * Az időpontok ISO 8601 szövegként érkeznek: az Instant UTC-ben ("2026-12-01T09:00:00Z"),
 * a LocalDate "2026-12-01", a LocalTime "09:00" vagy "09:00:00" alakban.
 */

export type Role = 'CUSTOMER' | 'BARBER' | 'OWNER';

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  mustChangePassword: boolean;
  /** A borbély saját azonosítója (a beosztás és a naptár ehhez tartozik); vendégnél és tulajdonosnál null. */
  barberId: number | null;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  fullName: string;
  password: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ServiceOffering {
  id: number;
  name: string;
  description: string | null;
  durationMinutes: number;
  priceHuf: number;
  active: boolean;
}

/** A borbély nyilvános adatai; bejelentkezés nélkül és a nem tulajdonos szerepkörök ezt kapják. */
export interface BarberPublic {
  id: number;
  fullName: string;
  bio: string | null;
  services: ServiceOffering[];
}

/** A borbély teljes adatai; csak a tulajdonos kapja. */
export interface BarberDetails extends BarberPublic {
  userId: number;
  email: string;
  active: boolean;
  minLeadMinutes: number;
}

export interface Slot {
  startAt: string;
  /** Helyi idő (Europe/Budapest), "HH:mm". */
  time: string;
}

export type BookingStatus = 'BOOKED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export interface Booking {
  id: number;
  barberId: number;
  barberName: string;
  customerId: number;
  customerName: string;
  serviceId: number;
  serviceName: string;
  startAt: string;
  endAt: string;
  priceHuf: number;
  status: BookingStatus;
  cancelledAt: string | null;
}

export interface BookingCreateRequest {
  barberId: number;
  serviceId: number;
  startAt: string;
  /** Csak a személyzet adja meg, ha vendég nevében foglal. */
  customerId?: number;
}

export interface Availability {
  id: number;
  barberId: number;
  workDate: string;
  startTime: string;
  endTime: string;
}

export interface TimeOff {
  id: number;
  barberId: number;
  startAt: string;
  endAt: string;
  reason: string | null;
}

export interface Customer {
  id: number;
  fullName: string;
  email: string;
}
