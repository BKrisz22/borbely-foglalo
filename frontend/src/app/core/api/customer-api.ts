import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Customer } from './api.models';

/** Vendégkereső a személyzetnek (telefonos foglalás vendég nevében). */
@Injectable({ providedIn: 'root' })
export class CustomerApi {
  private readonly http = inject(HttpClient);

  /** Név- vagy e-mail-részlet alapján, legalább 2 karakterrel; legfeljebb 20 találat. */
  search(query: string): Observable<Customer[]> {
    return this.http.get<Customer[]>('/api/customers', { params: { query } });
  }
}
