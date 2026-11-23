import { Injectable, computed, inject, signal } from '@angular/core';

import { BarberPublic } from '../../core/api/api.models';
import { CatalogApi } from '../../core/api/catalog-api';
import { AuthService } from '../../core/auth/auth.service';
import { toProblem } from '../../core/http/problem';

/**
 * Kinek a naptárát és beosztását mutatja a személyzeti felület. A borbély mindig a sajátját;
 * a tulajdonos választ a borbélyok közül, és a választás a két oldal között megmarad.
 */
@Injectable({ providedIn: 'root' })
export class StaffBarberContext {
  private readonly auth = inject(AuthService);
  private readonly catalog = inject(CatalogApi);

  /** A tulajdonos által választható borbélyok; null, amíg nincs betöltve. */
  readonly barbers = signal<BarberPublic[] | null>(null);
  readonly error = signal<string | null>(null);
  private readonly chosen = signal<number | null>(null);
  private loading = false;

  readonly isOwner = computed(() => this.auth.role() === 'OWNER');

  readonly barberId = computed<number | null>(() => {
    const user = this.auth.user();
    if (user?.role === 'BARBER') {
      return user.barberId;
    }
    if (user?.role !== 'OWNER') {
      return null;
    }
    const barbers = this.barbers() ?? [];
    const chosen = this.chosen();
    // Ha a korábban választott borbélyt azóta kikapcsolták, az első aktív jön.
    return barbers.some((barber) => barber.id === chosen) ? chosen : (barbers[0]?.id ?? null);
  });

  /** A tulajdonosnak betölti a borbélyok listáját (csak egyszer). */
  load(): void {
    if (!this.isOwner() || this.barbers() !== null || this.loading) {
      return;
    }
    this.loading = true;
    this.error.set(null);
    this.catalog.barbers().subscribe({
      next: (barbers) => {
        this.loading = false;
        this.barbers.set(barbers);
      },
      error: (error: unknown) => {
        this.loading = false;
        this.error.set(toProblem(error).message);
      },
    });
  }

  select(barberId: number): void {
    this.chosen.set(barberId);
  }
}
