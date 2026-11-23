import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Observable, map } from 'rxjs';

import { Booking } from '../../core/api/api.models';
import { shopDate, shopTime } from '../../core/time/shop-time';
import { STATUS_LABELS } from '../my-bookings/booking-view';
import { BookingAction } from './calendar-events';

export interface BookingDetailsData {
  booking: Booking;
  /** A most elérhető műveletek (staffActions). */
  actions: BookingAction[];
  /** A tulajdonosnak a borbély neve is látszik. */
  showBarber: boolean;
}

/** Egy foglalás adatai a naptárból; a választott művelettel (vagy semmivel) zárul. */
@Component({
  selector: 'app-booking-details-dialog',
  imports: [CurrencyPipe, MatButtonModule, MatDialogModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>{{ booking.customerName }}</h2>
    <mat-dialog-content>
      <dl class="details">
        <dt>Időpont</dt>
        <dd>
          {{ shopDate(booking.startAt) }}, {{ shopTime(booking.startAt) }}–{{
            shopTime(booking.endAt)
          }}
        </dd>
        <dt>Szolgáltatás</dt>
        <dd>
          {{ booking.serviceName }} ·
          {{ booking.priceHuf | currency: 'HUF' : 'symbol' : '1.0-0' }}
        </dd>
        @if (data.showBarber) {
          <dt>Borbély</dt>
          <dd>{{ booking.barberName }}</dd>
        }
        <dt>Állapot</dt>
        <dd>{{ statusLabels[booking.status] }}</dd>
      </dl>
      @if (data.actions.includes('complete')) {
        <p class="hint">A foglalás elkezdődött: jelöld, hogy a vendég megjelent-e.</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton type="button" mat-dialog-close>Bezárás</button>
      @if (data.actions.includes('cancel')) {
        <button matButton="outlined" type="button" [mat-dialog-close]="'cancel'">
          <mat-icon>event_busy</mat-icon>
          Lemondás
        </button>
      }
      @if (data.actions.includes('noShow')) {
        <button matButton="outlined" type="button" [mat-dialog-close]="'noShow'">
          <mat-icon>person_off</mat-icon>
          Nem jelent meg
        </button>
      }
      @if (data.actions.includes('complete')) {
        <button matButton="filled" type="button" [mat-dialog-close]="'complete'" cdkFocusInitial>
          <mat-icon>task_alt</mat-icon>
          Megjelent
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: `
    .details {
      display: grid;
      grid-template-columns: auto 1fr;
      gap: 8px 16px;
      margin: 0;
    }

    dt {
      color: var(--mat-sys-on-surface-variant);
    }

    dd {
      margin: 0;
    }

    .hint {
      margin: 16px 0 0;
      color: var(--mat-sys-on-surface-variant);
    }
  `,
})
export class BookingDetailsDialog {
  protected readonly data = inject<BookingDetailsData>(MAT_DIALOG_DATA);
  protected readonly booking = this.data.booking;
  protected readonly statusLabels = STATUS_LABELS;
  protected readonly shopDate = shopDate;
  protected readonly shopTime = shopTime;
}

/** Megnyitja a foglalás adatait; a választott művelet, vagy undefined, ha csak bezárták. */
export function openBookingDetails(
  dialog: MatDialog,
  data: BookingDetailsData,
): Observable<BookingAction | undefined> {
  return dialog
    .open<BookingDetailsDialog, BookingDetailsData, BookingAction>(BookingDetailsDialog, {
      data,
      width: '460px',
    })
    .afterClosed()
    .pipe(map((result) => (isBookingAction(result) ? result : undefined)));
}

/** A sima mat-dialog-close üres szöveggel zár; csak a valódi művelet számít. */
function isBookingAction(value: unknown): value is BookingAction {
  return value === 'cancel' || value === 'complete' || value === 'noShow';
}
