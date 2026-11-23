import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Availability,
  AvailabilitySlotRequest,
  AvailabilityUpdateRequest,
  TimeOff,
  TimeOffCreateRequest,
} from './api.models';

/**
 * A borbély beosztása: munkaidő-sávok (AvailabilityController) és távollétek (TimeOffController).
 * A borbély a sajátját, a tulajdonos bárkiét kezelheti; a lekérdezés legfeljebb 93 napot fog át.
 */
@Injectable({ providedIn: 'root' })
export class ScheduleApi {
  private readonly http = inject(HttpClient);

  /** A munkaidő-sávok két nap között (mindkettőt beleértve). */
  availability(barberId: number, from: string, to: string): Observable<Availability[]> {
    return this.http.get<Availability[]>(`/api/barbers/${barberId}/availability`, {
      params: { from, to },
    });
  }

  /** Egy vagy több sáv felvétele; vagy mind bekerül, vagy egyik sem. */
  createAvailability(
    barberId: number,
    slots: AvailabilitySlotRequest[],
  ): Observable<Availability[]> {
    return this.http.post<Availability[]>(`/api/barbers/${barberId}/availability`, { slots });
  }

  /** Az egyik hét sávjainak átmásolása egy másik hétre; mindkét dátum hétfő ("2026-11-23"). */
  copyWeek(
    barberId: number,
    sourceWeekStart: string,
    targetWeekStart: string,
  ): Observable<Availability[]> {
    return this.http.post<Availability[]>(`/api/barbers/${barberId}/availability/copy-week`, {
      sourceWeekStart,
      targetWeekStart,
    });
  }

  /** Egy sáv idejének módosítása; a nap nem változhat. */
  updateAvailability(
    barberId: number,
    id: number,
    request: AvailabilityUpdateRequest,
  ): Observable<Availability> {
    return this.http.put<Availability>(`/api/barbers/${barberId}/availability/${id}`, request);
  }

  deleteAvailability(barberId: number, id: number): Observable<void> {
    return this.http.delete<void>(`/api/barbers/${barberId}/availability/${id}`);
  }

  /** A két nap (helyi idő szerint, mindkettőt beleértve) közé eső távollétek. */
  timeOffs(barberId: number, from: string, to: string): Observable<TimeOff[]> {
    return this.http.get<TimeOff[]>(`/api/barbers/${barberId}/time-off`, { params: { from, to } });
  }

  createTimeOff(barberId: number, request: TimeOffCreateRequest): Observable<TimeOff> {
    return this.http.post<TimeOff>(`/api/barbers/${barberId}/time-off`, request);
  }

  deleteTimeOff(barberId: number, id: number): Observable<void> {
    return this.http.delete<void>(`/api/barbers/${barberId}/time-off/${id}`);
  }
}
