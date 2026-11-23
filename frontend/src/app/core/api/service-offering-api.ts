import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ServiceOffering, ServiceOfferingRequest } from './api.models';

/**
 * A szolgáltatások kezelése a tulajdonosnak (ServiceOfferingController). A nyilvános,
 * csak aktív szolgáltatásokat adó lista a CatalogApi-ban van.
 */
@Injectable({ providedIn: 'root' })
export class ServiceOfferingApi {
  private readonly http = inject(HttpClient);

  /** Az összes szolgáltatás név szerint, a kivezetettekkel együtt. */
  listAll(): Observable<ServiceOffering[]> {
    return this.http.get<ServiceOffering[]>('/api/services', {
      params: { includeInactive: true },
    });
  }

  create(request: ServiceOfferingRequest): Observable<ServiceOffering> {
    return this.http.post<ServiceOffering>('/api/services', request);
  }

  /** Módosítás; az active: true visszaveszi a kivezetett szolgáltatást. */
  update(id: number, request: ServiceOfferingRequest): Observable<ServiceOffering> {
    return this.http.put<ServiceOffering>(`/api/services/${id}`, request);
  }

  /** Kivezetés: nem töröl, a korábbi foglalások hivatkozása megmarad. */
  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`/api/services/${id}`);
  }
}
