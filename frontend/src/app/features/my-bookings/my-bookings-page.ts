import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';

import { Booking } from '../../core/api/api.models';
import { BookingApi } from '../../core/api/booking-api';
import { toProblem } from '../../core/http/problem';
import { addDays, startOfDay, toIsoDate } from '../../core/time/local-date';
import { shopDate, shopTime } from '../../core/time/shop-time';
import { confirmAction } from '../../core/ui/confirm-dialog';
import { Notifier } from '../../core/ui/notifier';
import {
  CANCEL_DEADLINE_HOURS,
  STATUS_LABELS,
  canCustomerCancel,
  splitBookings,
} from './booking-view';

/** Ennyi napra visszamenőleg mutatja a korábbi foglalásokat (a lekérdezés legfeljebb 93 napot fog át). */
const HISTORY_DAYS = 60;
/** Előre a foglalási ablak (30 nap) plusz egy nap ráhagyás. */
const AHEAD_DAYS = 31;

/** A vendég foglalásai: közelgők lemondással, alatta az elmúlt 60 nap. */
@Component({
  selector: 'app-my-bookings-page',
  imports: [
    CurrencyPipe,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './my-bookings-page.html',
  styleUrl: './my-bookings-page.scss',
})
export class MyBookingsPage {
  private readonly api = inject(BookingApi);
  private readonly dialog = inject(MatDialog);
  private readonly notifier = inject(Notifier);

  protected readonly historyDays = HISTORY_DAYS;
  protected readonly cancelDeadlineHours = CANCEL_DEADLINE_HOURS;
  protected readonly statusLabels = STATUS_LABELS;
  protected readonly shopDate = shopDate;
  protected readonly shopTime = shopTime;

  protected readonly bookings = signal<Booking[] | null>(null);
  protected readonly error = signal<string | null>(null);
  /** A lemondás alatt álló foglalás azonosítója (a gombja addig tiltott). */
  protected readonly cancelling = signal<number | null>(null);
  /** A „most” a lista betöltésekor és minden változáskor frissül. */
  private readonly now = signal(Date.now());

  protected readonly split = computed(() => splitBookings(this.bookings() ?? [], this.now()));

  constructor() {
    const today = startOfDay();
    this.api
      .list(toIsoDate(addDays(today, -HISTORY_DAYS)), toIsoDate(addDays(today, AHEAD_DAYS)))
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (bookings) => {
          this.now.set(Date.now());
          this.bookings.set(bookings);
        },
        error: (error: unknown) => this.error.set(toProblem(error).message),
      });
  }

  protected canCancel(booking: Booking): boolean {
    return canCustomerCancel(booking, this.now());
  }

  protected cancel(booking: Booking): void {
    confirmAction(this.dialog, {
      title: 'Lemondod a foglalást?',
      message: `${booking.serviceName} – ${booking.barberName}, ${shopDate(booking.startAt)} ${shopTime(booking.startAt)}`,
      confirmLabel: 'Lemondás',
      cancelLabel: 'Mégse',
    })
      .pipe(
        filter((confirmed) => confirmed),
        switchMap(() => {
          this.cancelling.set(booking.id);
          return this.api.cancel(booking.id);
        }),
      )
      .subscribe({
        next: (updated) => {
          this.cancelling.set(null);
          this.now.set(Date.now());
          this.bookings.update((list) =>
            (list ?? []).map((item) => (item.id === updated.id ? updated : item)),
          );
          this.notifier.info('Lemondtad a foglalást.');
        },
        error: (error: unknown) => {
          this.cancelling.set(null);
          this.notifier.error(toProblem(error).message);
        },
      });
  }
}
