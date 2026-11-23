import { Component, effect, inject, signal, untracked, viewChild } from '@angular/core';
import {
  CalendarOptions,
  EventClickInfo,
  EventInput,
  EventSourceFuncInfo,
  FullCalendarComponent,
  FullCalendarModule,
} from '@fullcalendar/angular';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { EMPTY, Observable, filter, firstValueFrom, forkJoin, map, switchMap } from 'rxjs';

import { Booking } from '../../core/api/api.models';
import { BookingApi } from '../../core/api/booking-api';
import { ScheduleApi } from '../../core/api/schedule-api';
import { toProblem } from '../../core/http/problem';
import { toIsoDate } from '../../core/time/local-date';
import { shopDate, shopTime } from '../../core/time/shop-time';
import { confirmAction } from '../../core/ui/confirm-dialog';
import { Notifier } from '../../core/ui/notifier';
import { BarberPicker } from './barber-picker';
import { openBookingDetails } from './booking-details-dialog';
import {
  BookingAction,
  availabilityEvent,
  bookingEvent,
  entryOf,
  shiftDate,
  staffActions,
  timeOffEvent,
  wallClock,
} from './calendar-events';
import { baseCalendarOptions } from './calendar-options';
import { StaffBarberContext } from './staff-barber';

const DONE_MESSAGES: Readonly<Record<BookingAction, string>> = {
  cancel: 'A foglalást lemondtad.',
  complete: 'Lezárva: a vendég megjelent.',
  noShow: 'Lezárva: a vendég nem jelent meg.',
};

/**
 * A borbély heti naptára: a foglalások kattintható blokkok, a munkaidő és a távollét háttérszín.
 * Egy foglalásra kattintva kezdés előtt lemondható, utána lezárható (megjelent, nem jelent meg).
 */
@Component({
  selector: 'app-calendar-page',
  imports: [FullCalendarModule, RouterLink, MatButtonModule, MatIconModule, BarberPicker],
  templateUrl: './calendar-page.html',
  styleUrl: './staff-page.scss',
})
export class CalendarPage {
  private readonly bookings = inject(BookingApi);
  private readonly schedule = inject(ScheduleApi);
  private readonly dialog = inject(MatDialog);
  private readonly notifier = inject(Notifier);
  protected readonly context = inject(StaffBarberContext);

  private readonly calendar = viewChild(FullCalendarComponent);
  protected readonly error = signal<string | null>(null);

  protected readonly options: CalendarOptions = {
    ...baseCalendarOptions(),
    events: (info: EventSourceFuncInfo) => this.loadEvents(info),
    eventClick: (info: EventClickInfo) => this.onEventClick(info),
  };

  constructor() {
    // Másik borbély választásakor (tulajdonos) újratölt.
    effect(() => {
      this.context.barberId();
      untracked(() => this.refresh());
    });
  }

  private refresh(): void {
    // A FullCalendar csak a nézet felépülése után jön létre; addig nincs mit frissíteni.
    this.calendar()?.getApi()?.refetchEvents();
  }

  private loadEvents(info: EventSourceFuncInfo): Promise<EventInput[]> {
    const barberId = this.context.barberId();
    if (barberId === null) {
      return Promise.resolve([]);
    }
    // A FullCalendar vége kizáró, a backendé beleértő.
    const from = wallClock(info.startStr).date;
    const to = shiftDate(wallClock(info.endStr).date, -1);
    const today = toIsoDate(new Date());
    this.error.set(null);
    return firstValueFrom(
      forkJoin({
        bookings: this.bookings.list(from, to, barberId),
        availability: this.schedule.availability(barberId, from, to),
        timeOffs: this.schedule.timeOffs(barberId, from, to),
      }),
    ).then(
      ({ bookings, availability, timeOffs }) => [
        ...availability.map((slot) => availabilityEvent(slot, 'background', today)),
        ...timeOffs.map((timeOff) => timeOffEvent(timeOff, 'background')),
        ...bookings
          .filter((booking) => booking.status !== 'CANCELLED')
          .map((booking) => bookingEvent(booking)),
      ],
      (error: unknown) => {
        this.error.set(toProblem(error).message);
        return [];
      },
    );
  }

  private onEventClick(info: EventClickInfo): void {
    const entry = entryOf(info.event);
    if (entry?.kind === 'booking') {
      this.openBooking(entry.booking);
    }
  }

  private openBooking(booking: Booking): void {
    openBookingDetails(this.dialog, {
      booking,
      actions: staffActions(booking, Date.now()),
      showBarber: this.context.isOwner(),
    })
      .pipe(
        filter((action) => action !== undefined),
        switchMap((action) => this.run(booking, action)),
      )
      .subscribe({
        next: (action) => {
          this.notifier.info(DONE_MESSAGES[action]);
          this.refresh();
        },
        error: (error: unknown) => {
          this.notifier.error(toProblem(error).message);
          this.refresh();
        },
      });
  }

  /** Végrehajtja a műveletet; a lemondást előbb megerősítteti. */
  private run(booking: Booking, action: BookingAction): Observable<BookingAction> {
    if (action === 'complete') {
      return this.bookings.complete(booking.id).pipe(map(() => action));
    }
    if (action === 'noShow') {
      return this.bookings.noShow(booking.id).pipe(map(() => action));
    }
    return confirmAction(this.dialog, {
      title: 'Lemondod a foglalást?',
      message: `${booking.customerName}: ${booking.serviceName}, ${shopDate(booking.startAt)} ${shopTime(booking.startAt)}. Értesítsd a vendéget is.`,
      confirmLabel: 'Lemondás',
    }).pipe(
      switchMap((confirmed) =>
        confirmed ? this.bookings.cancel(booking.id).pipe(map(() => action)) : EMPTY,
      ),
    );
  }
}
