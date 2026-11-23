import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  BarberCreateRequest,
  BarberDetails,
  BarberProfileRequest,
  BarberUpdateRequest,
} from './api.models';

/**
 * A borbélyok kezelése (BarberController): a tulajdonos mindent, a borbély a saját profilját.
 * A nyilvános, csak aktív borbélyokat adó lista a CatalogApi-ban van.
 */
@Injectable({ providedIn: 'root' })
export class BarberApi {
  private readonly http = inject(HttpClient);

  /** Az összes borbély teljes adatokkal, a kikapcsoltakkal együtt (csak a tulajdonosnak). */
  listAll(): Observable<BarberDetails[]> {
    return this.http.get<BarberDetails[]>('/api/barbers', { params: { includeInactive: true } });
  }

  /** Egy borbély teljes adatai; a tulajdonos bárkiét, a borbély a sajátját kapja így. */
  get(id: number): Observable<BarberDetails> {
    return this.http.get<BarberDetails>(`/api/barbers/${id}`);
  }

  /** Új borbély kezdőjelszóval; az első belépéskor le kell cserélnie. */
  create(request: BarberCreateRequest): Observable<BarberDetails> {
    return this.http.post<BarberDetails>('/api/barbers', request);
  }

  /** Név, bemutatkozás, foglalási határidő; az active: true visszakapcsolja. */
  update(id: number, request: BarberUpdateRequest): Observable<BarberDetails> {
    return this.http.put<BarberDetails>(`/api/barbers/${id}`, request);
  }

  /** A vállalt szolgáltatások cseréje (az üres lista is érvényes). */
  replaceServices(id: number, serviceIds: number[]): Observable<BarberDetails> {
    return this.http.put<BarberDetails>(`/api/barbers/${id}/services`, { serviceIds });
  }

  /** A borbély saját profilja: bemutatkozás és foglalási határidő. */
  updateProfile(id: number, request: BarberProfileRequest): Observable<BarberDetails> {
    return this.http.put<BarberDetails>(`/api/barbers/${id}/profile`, request);
  }

  /** Kikapcsolás: nem töröl, a korábbi foglalásai megmaradnak. */
  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`/api/barbers/${id}`);
  }
}
