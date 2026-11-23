import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Booking, BookingCreateRequest, Slot } from './api.models';

/** Szabad időpontok (SlotController) és foglalások (BookingController). */
@Injectable({ providedIn: 'root' })
export class BookingApi {
  private readonly http = inject(HttpClient);

  /** A foglalható napok ("2026-12-01") két nap között; a backend a foglalási ablakra szűkít. */
  availableDays(
    barberId: number,
    serviceId: number,
    from: string,
    to: string,
  ): Observable<string[]> {
    return this.http.get<string[]>(`/api/barbers/${barberId}/available-days`, {
      params: { serviceId, from, to },
    });
  }

  /** Egy nap szabad kezdőpontjai 15 perces rácson, időrendben. */
  slots(barberId: number, serviceId: number, date: string): Observable<Slot[]> {
    return this.http.get<Slot[]>(`/api/barbers/${barberId}/slots`, { params: { serviceId, date } });
  }

  create(request: BookingCreateRequest): Observable<Booking> {
    return this.http.post<Booking>('/api/bookings', request);
  }

  /** Foglalások két nap között; a backend szerepkör szerint szűr (vendég: saját, borbély: saját naptár). */
  list(from: string, to: string, barberId?: number): Observable<Booking[]> {
    const params: Record<string, string | number> = { from, to };
    if (barberId !== undefined) {
      params['barberId'] = barberId;
    }
    return this.http.get<Booking[]>('/api/bookings', { params });
  }

  cancel(id: number): Observable<Booking> {
    return this.http.post<Booking>(`/api/bookings/${id}/cancel`, null);
  }

  /** Lezárás a kezdés után: a vendég megjelent (csak a személyzet). */
  complete(id: number): Observable<Booking> {
    return this.http.post<Booking>(`/api/bookings/${id}/complete`, null);
  }

  /** Lezárás a kezdés után: a vendég nem jelent meg (csak a személyzet). */
  noShow(id: number): Observable<Booking> {
    return this.http.post<Booking>(`/api/bookings/${id}/no-show`, null);
  }
}
