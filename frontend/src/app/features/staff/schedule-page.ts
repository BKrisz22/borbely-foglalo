import { Component, computed, effect, inject, signal, untracked, viewChild } from '@angular/core';
import {
  CalendarOptions,
  DateSelectInfo,
  DateSpanApi,
  DatesSetInfo,
  EventApi,
  EventChangeInfo,
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
import { Observable, filter, firstValueFrom, forkJoin, map, switchMap, tap } from 'rxjs';

import { Availability, TimeOff } from '../../core/api/api.models';
import { BookingApi } from '../../core/api/booking-api';
import { ScheduleApi } from '../../core/api/schedule-api';
import { toProblem } from '../../core/http/problem';
import { toIsoDate } from '../../core/time/local-date';
import { shopDate, shopTime } from '../../core/time/shop-time';
import { confirmAction } from '../../core/ui/confirm-dialog';
import { Notifier } from '../../core/ui/notifier';
import { BarberPicker } from './barber-picker';
import {
  WallClock,
  availabilityEvent,
  availabilityRequest,
  bookingEvent,
  entryOf,
  hhmm,
  instantOf,
  mondayOf,
  rangeLabel,
  shiftDate,
  timeOffEvent,
  wallClock,
  weekLabel,
  withShopOffset,
} from './calendar-events';
import { baseCalendarOptions } from './calendar-options';
import { NewEntryResult, openNewEntry } from './new-entry-dialog';
import { StaffBarberContext } from './staff-barber';

/**
 * A borbély beosztása heti rácson: kijelöléssel munkaidő vagy távollét vehető fel 15 perces
 * pontossággal, a munkaidő a napján belül húzható és nyújtható, kattintásra törölhető.
 * A foglalások szürke háttérként látszanak, hogy a módosítás ne vágja ki őket.
 */
@Component({
  selector: 'app-schedule-page',
  imports: [FullCalendarModule, RouterLink, MatButtonModule, MatIconModule, BarberPicker],
  templateUrl: './schedule-page.html',
  styleUrl: './staff-page.scss',
})
export class SchedulePage {
  private readonly bookings = inject(BookingApi);
  private readonly schedule = inject(ScheduleApi);
  private readonly dialog = inject(MatDialog);
  private readonly notifier = inject(Notifier);
  protected readonly context = inject(StaffBarberContext);

  private readonly calendar = viewChild(FullCalendarComponent);
  protected readonly error = signal<string | null>(null);
  protected readonly copying = signal(false);

  /** A látott időszak első napja ("2026-11-23"); hétnézetben hétfő. */
  private readonly visibleStart = signal<string | null>(null);
  /** A hét, amelyre az előző hét munkaideje másolódna. */
  protected readonly targetWeek = computed(() => {
    const start = this.visibleStart();
    return start === null ? null : mondayOf(start);
  });
  /** Csak a mostani vagy egy későbbi hétre lehet másolni. */
  protected readonly canCopy = computed(() => {
    const target = this.targetWeek();
    return (
      target !== null &&
      target >= mondayOf(toIsoDate(new Date())) &&
      this.context.barberId() !== null &&
      !this.copying()
    );
  });
  protected readonly weekLabel = weekLabel;

  protected readonly options: CalendarOptions = {
    ...baseCalendarOptions(),
    selectable: true,
    selectMirror: true,
    snapDuration: '00:15',
    events: (info: EventSourceFuncInfo) => this.loadEvents(info),
    datesSet: (info: DatesSetInfo) => this.visibleStart.set(wallClock(info.startStr).date),
    // Múltba nem lehet munkaidőt vagy távollétet felvenni.
    selectAllow: (span: DateSpanApi) => instantOf(span.startStr) >= Date.now(),
    eventAllow: (span: DateSpanApi, moving: EventApi | null) => this.canMoveTo(span, moving),
    select: (info: DateSelectInfo) => this.onSelect(info),
    eventClick: (info: EventClickInfo) => this.onEventClick(info),
    eventDrop: (info: EventChangeInfo) => this.onChange(info),
    eventResize: (info: EventChangeInfo) => this.onChange(info),
  };

  constructor() {
    effect(() => {
      this.context.barberId();
      untracked(() => this.refresh());
    });
  }

  protected copyPreviousWeek(): void {
    const target = this.targetWeek();
    const barberId = this.context.barberId();
    if (target === null || barberId === null) {
      return;
    }
    const source = shiftDate(target, -7);
    confirmAction(this.dialog, {
      title: 'Előző hét másolása',
      message:
        `Az előző hét (${weekLabel(source)}) munkaidő-sávjai erre a hétre (${weekLabel(target)}) ` +
        'kerülnek, ugyanarra a napra és órára. Ha bármelyik ütközik egy meglévővel, ' +
        'egyik sem másolódik át.',
      confirmLabel: 'Másolás',
    })
      .pipe(
        filter((confirmed) => confirmed),
        switchMap(() => {
          this.copying.set(true);
          return this.schedule.copyWeek(barberId, source, target);
        }),
      )
      .subscribe({
        next: (slots) => {
          this.copying.set(false);
          this.notifier.info(`${slots.length} munkaidő-sáv átmásolva.`);
          this.refresh();
        },
        error: (error: unknown) => {
          this.copying.set(false);
          this.notifier.error(toProblem(error).message);
        },
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
    const from = wallClock(info.startStr).date;
    const to = shiftDate(wallClock(info.endStr).date, -1);
    const today = toIsoDate(new Date());
    this.error.set(null);
    return firstValueFrom(
      forkJoin({
        availability: this.schedule.availability(barberId, from, to),
        timeOffs: this.schedule.timeOffs(barberId, from, to),
        bookings: this.bookings.list(from, to, barberId),
      }),
    ).then(
      ({ availability, timeOffs, bookings }) => [
        ...availability.map((slot) => availabilityEvent(slot, 'block', today)),
        ...timeOffs.map((timeOff) => timeOffEvent(timeOff, 'block')),
        ...bookings
          .filter((booking) => booking.status !== 'CANCELLED')
          .map((booking) => bookingEvent(booking, 'background')),
      ],
      (error: unknown) => {
        this.error.set(toProblem(error).message);
        return [];
      },
    );
  }

  /** A munkaidő csak a saját napján belül mozgatható (a backend a napot nem módosítja). */
  private canMoveTo(span: DateSpanApi, moving: EventApi | null): boolean {
    const entry = moving ? entryOf(moving) : null;
    if (entry?.kind !== 'availability') {
      return false;
    }
    const day = entry.availability.workDate;
    return wallClock(span.startStr).date === day && wallClock(span.endStr).date === day;
  }

  private onSelect(info: DateSelectInfo): void {
    const barberId = this.context.barberId();
    if (barberId === null) {
      return;
    }
    const start = wallClock(info.startStr);
    const end = wallClock(info.endStr);
    openNewEntry(this.dialog, {
      label: rangeLabel(start, end),
      sameDay: availabilityRequest(start, end) !== null,
    })
      .pipe(
        tap(() => this.calendar()?.getApi()?.unselect()),
        filter((result) => result !== undefined),
        switchMap((result) => this.create(barberId, result, start, end, info)),
      )
      .subscribe({
        next: (message) => {
          this.notifier.info(message);
          this.refresh();
        },
        error: (error: unknown) => this.notifier.error(toProblem(error).message),
      });
  }

  private create(
    barberId: number,
    result: NewEntryResult,
    start: WallClock,
    end: WallClock,
    info: DateSelectInfo,
  ): Observable<string> {
    const slot = availabilityRequest(start, end);
    if (result.kind === 'availability' && slot !== null) {
      return this.schedule
        .createAvailability(barberId, [slot])
        .pipe(map(() => `Munkaidő felvéve: ${rangeLabel(start, end)}.`));
    }
    return this.schedule
      .createTimeOff(barberId, {
        startAt: withShopOffset(info.startStr),
        endAt: withShopOffset(info.endStr),
        reason: result.kind === 'timeOff' ? result.reason : null,
      })
      .pipe(map(() => `Távollét felvéve: ${rangeLabel(start, end)}.`));
  }

  private onEventClick(info: EventClickInfo): void {
    const entry = entryOf(info.event);
    if (entry?.kind === 'availability') {
      this.deleteAvailability(entry.availability);
    } else if (entry?.kind === 'timeOff') {
      this.deleteTimeOff(entry.timeOff);
    }
  }

  private deleteAvailability(slot: Availability): void {
    const label = rangeLabel(
      { date: slot.workDate, time: hhmm(slot.startTime) },
      { date: slot.workDate, time: hhmm(slot.endTime) },
    );
    this.confirmDelete('Törlöd a munkaidőt?', label, () =>
      this.schedule.deleteAvailability(slot.barberId, slot.id),
    );
  }

  private deleteTimeOff(timeOff: TimeOff): void {
    const startDate = shopDate(timeOff.startAt);
    const endDate = shopDate(timeOff.endAt);
    const range =
      startDate === endDate
        ? `${startDate}, ${shopTime(timeOff.startAt)}–${shopTime(timeOff.endAt)}`
        : `${startDate} ${shopTime(timeOff.startAt)} – ${endDate} ${shopTime(timeOff.endAt)}`;
    const label = range + (timeOff.reason ? ` (${timeOff.reason})` : '');
    this.confirmDelete('Törlöd a távollétet?', label, () =>
      this.schedule.deleteTimeOff(timeOff.barberId, timeOff.id),
    );
  }

  private confirmDelete(title: string, message: string, remove: () => Observable<void>): void {
    confirmAction(this.dialog, { title, message, confirmLabel: 'Törlés' })
      .pipe(
        filter((confirmed) => confirmed),
        switchMap(() => remove()),
      )
      .subscribe({
        next: () => {
          this.notifier.info('Törölve.');
          this.refresh();
        },
        error: (error: unknown) => this.notifier.error(toProblem(error).message),
      });
  }

  /** Húzás vagy nyújtás után menti az új időt; hibánál visszaugrik. */
  private onChange(info: EventChangeInfo): void {
    const entry = entryOf(info.event);
    const barberId = this.context.barberId();
    if (entry?.kind !== 'availability' || barberId === null) {
      info.revert();
      return;
    }
    const start = wallClock(info.event.startStr);
    const end = wallClock(info.event.endStr);
    const slot = availabilityRequest(start, end);
    if (slot === null || slot.workDate !== entry.availability.workDate) {
      info.revert();
      this.notifier.error(
        'A munkaidő csak a napján belül mozgatható; másik napra törléssel és új sávval teheted át.',
      );
      return;
    }
    this.schedule
      .updateAvailability(barberId, entry.availability.id, {
        startTime: slot.startTime,
        endTime: slot.endTime,
      })
      .subscribe({
        next: () => {
          this.notifier.info(`Munkaidő módosítva: ${rangeLabel(start, end)}.`);
          this.refresh();
        },
        error: (error: unknown) => {
          info.revert();
          this.notifier.error(toProblem(error).message);
        },
      });
  }
}
