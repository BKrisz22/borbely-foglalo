import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { filter, forkJoin, switchMap } from 'rxjs';

import { BarberDetails, ServiceOffering } from '../../core/api/api.models';
import { BarberApi } from '../../core/api/barber-api';
import { ServiceOfferingApi } from '../../core/api/service-offering-api';
import { toProblem } from '../../core/http/problem';
import { leadTimeLabel } from '../../core/time/lead-time';
import { confirmAction } from '../../core/ui/confirm-dialog';
import { Notifier } from '../../core/ui/notifier';
import { StaffBarberContext } from '../staff/staff-barber';
import { openBarberDialog } from './barber-dialog';

/**
 * A tulajdonos borbélylistája: felvétel kezdőjelszóval, adatok és vállalt szolgáltatások
 * módosítása, kikapcsolás és visszakapcsolás. Törlés nincs, a korábbi foglalások megmaradnak.
 */
@Component({
  selector: 'app-barbers-page',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './barbers-page.html',
  styleUrl: './admin-page.scss',
})
export class BarbersPage {
  private readonly barberApi = inject(BarberApi);
  private readonly serviceApi = inject(ServiceOfferingApi);
  private readonly staffBarbers = inject(StaffBarberContext);
  private readonly dialog = inject(MatDialog);
  private readonly notifier = inject(Notifier);

  protected readonly leadTimeLabel = leadTimeLabel;
  protected readonly barbers = signal<BarberDetails[] | null>(null);
  private readonly services = signal<ServiceOffering[]>([]);
  protected readonly error = signal<string | null>(null);
  /** A művelet alatt álló borbély azonosítója (a gombjai addig tiltottak). */
  protected readonly busy = signal<number | null>(null);

  constructor() {
    this.load();
  }

  protected serviceNames(barber: BarberDetails): string {
    return barber.services.map((service) => service.name).join(', ');
  }

  protected add(): void {
    openBarberDialog(this.dialog, { barber: null, services: this.services() }).subscribe(
      (saved) => {
        if (saved) {
          this.changed(
            `Felvetted: ${saved.fullName}. Az első belépéskor új jelszót kell megadnia.`,
          );
        }
      },
    );
  }

  protected edit(barber: BarberDetails): void {
    openBarberDialog(this.dialog, { barber, services: this.services() }).subscribe((saved) => {
      if (saved) {
        this.changed(`Mentve: ${saved.fullName}.`);
      }
    });
  }

  protected deactivate(barber: BarberDetails): void {
    confirmAction(this.dialog, {
      title: 'Kikapcsolod a borbélyt?',
      message:
        `${barber.fullName} ezután nem foglalható, és nem jelenik meg a vendégeknek. A korábbi ` +
        'foglalásai megmaradnak; a jövőbelieket előtte érdemes a Naptárban lemondani. ' +
        'Később visszakapcsolhatod.',
      confirmLabel: 'Kikapcsolás',
    })
      .pipe(
        filter((confirmed) => confirmed),
        switchMap(() => {
          this.busy.set(barber.id);
          return this.barberApi.deactivate(barber.id);
        }),
      )
      .subscribe({
        next: () => this.changed(`Kikapcsolva: ${barber.fullName}.`),
        error: (error: unknown) => this.failed(error),
      });
  }

  protected reactivate(barber: BarberDetails): void {
    this.busy.set(barber.id);
    this.barberApi
      .update(barber.id, {
        fullName: barber.fullName,
        bio: barber.bio,
        minLeadMinutes: barber.minLeadMinutes,
        active: true,
      })
      .subscribe({
        next: () => this.changed(`Újra foglalható: ${barber.fullName}.`),
        error: (error: unknown) => this.failed(error),
      });
  }

  private load(): void {
    forkJoin({ barbers: this.barberApi.listAll(), services: this.serviceApi.listAll() }).subscribe({
      next: ({ barbers, services }) => {
        this.error.set(null);
        this.services.set(services);
        this.barbers.set(barbers);
      },
      error: (error: unknown) => this.error.set(toProblem(error).message),
    });
  }

  /** Sikeres változtatás után: üzenet, újratöltés, és a naptár borbélyválasztója is frissül. */
  private changed(message: string): void {
    this.busy.set(null);
    this.notifier.info(message);
    this.staffBarbers.invalidate();
    this.load();
  }

  private failed(error: unknown): void {
    this.busy.set(null);
    this.notifier.error(toProblem(error).message);
  }
}
