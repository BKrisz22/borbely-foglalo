import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { filter, switchMap } from 'rxjs';

import { ServiceOffering } from '../../core/api/api.models';
import { ServiceOfferingApi } from '../../core/api/service-offering-api';
import { toProblem } from '../../core/http/problem';
import { confirmAction } from '../../core/ui/confirm-dialog';
import { Notifier } from '../../core/ui/notifier';
import { openServiceDialog } from './service-dialog';

/**
 * A tulajdonos szolgáltatáslistája: felvétel, módosítás, kivezetés és visszavétel.
 * Törlés nincs, hogy a korábbi foglalások hivatkozása megmaradjon.
 */
@Component({
  selector: 'app-services-page',
  imports: [
    CurrencyPipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './services-page.html',
  styleUrl: './admin-page.scss',
})
export class ServicesPage {
  private readonly api = inject(ServiceOfferingApi);
  private readonly dialog = inject(MatDialog);
  private readonly notifier = inject(Notifier);

  protected readonly services = signal<ServiceOffering[] | null>(null);
  protected readonly error = signal<string | null>(null);
  /** A művelet alatt álló szolgáltatás azonosítója (a gombjai addig tiltottak). */
  protected readonly busy = signal<number | null>(null);

  constructor() {
    this.load();
  }

  protected add(): void {
    openServiceDialog(this.dialog, null).subscribe((saved) => {
      if (saved) {
        this.notifier.info(`Felvetted: ${saved.name}.`);
        this.load();
      }
    });
  }

  protected edit(service: ServiceOffering): void {
    openServiceDialog(this.dialog, service).subscribe((saved) => {
      if (saved) {
        this.notifier.info(`Mentve: ${saved.name}.`);
        this.load();
      }
    });
  }

  protected deactivate(service: ServiceOffering): void {
    confirmAction(this.dialog, {
      title: 'Kivezeted a szolgáltatást?',
      message:
        `„${service.name}” ezután nem foglalható. A már lefoglalt és a korábbi időpontok ` +
        'megmaradnak, és később visszaveheted.',
      confirmLabel: 'Kivezetés',
    })
      .pipe(
        filter((confirmed) => confirmed),
        switchMap(() => {
          this.busy.set(service.id);
          return this.api.deactivate(service.id);
        }),
      )
      .subscribe({
        next: () => this.done(`Kivezetve: ${service.name}.`),
        error: (error: unknown) => this.failed(error),
      });
  }

  protected reactivate(service: ServiceOffering): void {
    this.busy.set(service.id);
    this.api
      .update(service.id, {
        name: service.name,
        description: service.description,
        durationMinutes: service.durationMinutes,
        priceHuf: service.priceHuf,
        active: true,
      })
      .subscribe({
        next: () => this.done(`Újra foglalható: ${service.name}.`),
        error: (error: unknown) => this.failed(error),
      });
  }

  private load(): void {
    this.api.listAll().subscribe({
      next: (services) => {
        this.error.set(null);
        this.services.set(services);
      },
      error: (error: unknown) => this.error.set(toProblem(error).message),
    });
  }

  private done(message: string): void {
    this.busy.set(null);
    this.notifier.info(message);
    this.load();
  }

  private failed(error: unknown): void {
    this.busy.set(null);
    this.notifier.error(toProblem(error).message);
  }
}
