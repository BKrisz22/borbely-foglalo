import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { BarberPublic, ServiceOffering } from './api.models';

/** A nyilvános kínálat: szolgáltatások és borbélyok (belépés nélkül is elérhető). */
@Injectable({ providedIn: 'root' })
export class CatalogApi {
  private readonly http = inject(HttpClient);

  /** Az aktív szolgáltatások név szerint rendezve. */
  services(): Observable<ServiceOffering[]> {
    return this.http.get<ServiceOffering[]>('/api/services');
  }

  /** Az aktív borbélyok; ha szolgáltatás is meg van adva, csak azok, akik vállalják. */
  barbers(serviceId?: number): Observable<BarberPublic[]> {
    const params: Record<string, number> = serviceId === undefined ? {} : { serviceId };
    return this.http.get<BarberPublic[]>('/api/barbers', { params });
  }
}
