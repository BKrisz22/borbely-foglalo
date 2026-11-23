import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { BarberPublic, ServiceOffering } from '../../core/api/api.models';
import { CatalogApi } from '../../core/api/catalog-api';
import { AuthService } from '../../core/auth/auth.service';
import { toProblem } from '../../core/http/problem';

/** Nyilvános kezdőlap: a kínálat (szolgáltatások, borbélyok) és a foglalás indítása. */
@Component({
  selector: 'app-home-page',
  imports: [
    CurrencyPipe,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss',
})
export class HomePage {
  private readonly auth = inject(AuthService);

  protected readonly services = signal<ServiceOffering[] | null>(null);
  protected readonly barbers = signal<BarberPublic[] | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly isStaff = computed(() => {
    const role = this.auth.role();
    return role === 'BARBER' || role === 'OWNER';
  });
  protected readonly isLoggedIn = this.auth.isLoggedIn;

  constructor() {
    const catalog = inject(CatalogApi);
    forkJoin([catalog.services(), catalog.barbers()])
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: ([services, barbers]) => {
          this.services.set(services);
          this.barbers.set(barbers);
        },
        error: (error: unknown) => this.error.set(toProblem(error).message),
      });
  }

  protected homeUrl(): string {
    return this.auth.homeUrl();
  }

  /** „Kovács Péter” → „KP”. */
  protected initials(name: string): string {
    return name
      .split(/\s+/)
      .filter((part) => part.length > 0)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
  }
}
