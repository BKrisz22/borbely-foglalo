import { CurrencyPipe, DatePipe, formatCurrency, formatDate } from '@angular/common';
import { Component, DestroyRef, LOCALE_ID, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import {
  MatAutocompleteModule,
  MatAutocompleteSelectedEvent,
} from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';

import { BarberPublic, Booking, Customer, ServiceOffering, Slot } from '../../core/api/api.models';
import { BookingApi } from '../../core/api/booking-api';
import { CatalogApi } from '../../core/api/catalog-api';
import { CustomerApi } from '../../core/api/customer-api';
import { AuthService } from '../../core/auth/auth.service';
import { toProblem } from '../../core/http/problem';
import { addDays, fromIsoDate, startOfDay, toIsoDate } from '../../core/time/local-date';
import {
  PendingSelection,
  addMinutesToTime,
  groupSlots,
  readPendingSelection,
  sameInstant,
} from './booking-selection';
import { StepHeader, StepState } from './step-header';

/** Hány napra előre lehet foglalni (a backend app.schedule.booking-horizon-days értéke; ott is ellenőrzött). */
const HORIZON_DAYS = 30;
/** Lemondási határidő órában (a backend app.booking.cancel-deadline értéke). */
const CANCEL_DEADLINE_HOURS = 24;

type Step = 0 | 1 | 2 | 3;

/**
 * Foglalási tölcsér: szolgáltatás → borbély → nap és időpont → összegzés. Belépés nélkül is
 * végigjárható; a foglaláshoz csak a végén kell belépni, a választás a címsorban megmarad.
 * A személyzet ugyanitt vendég nevében foglalhat (telefonos foglalás); a borbély csak a saját
 * naptárába.
 *
 * Az aktív lépés a választásokból következik (computed), így a „Módosítás” egyszerűen
 * törli az adott lépéstől kezdve a választást.
 */
@Component({
  selector: 'app-booking-page',
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatAutocompleteModule,
    MatButtonModule,
    MatCardModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    StepHeader,
  ],
  templateUrl: './booking-page.html',
  styleUrl: './booking-page.scss',
})
export class BookingPage {
  private readonly auth = inject(AuthService);
  private readonly catalog = inject(CatalogApi);
  private readonly bookings = inject(BookingApi);
  private readonly customers = inject(CustomerApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly locale = inject(LOCALE_ID);

  protected readonly horizonDays = HORIZON_DAYS;
  protected readonly cancelDeadlineHours = CANCEL_DEADLINE_HOURS;
  protected readonly today = startOfDay();
  protected readonly lastDay = addDays(this.today, HORIZON_DAYS);

  protected readonly isLoggedIn = this.auth.isLoggedIn;
  protected readonly isStaff = computed(() => {
    const role = this.auth.role();
    return role === 'BARBER' || role === 'OWNER';
  });

  protected readonly services = signal<ServiceOffering[] | null>(null);
  protected readonly service = signal<ServiceOffering | null>(null);
  protected readonly barbers = signal<BarberPublic[] | null>(null);
  protected readonly barber = signal<BarberPublic | null>(null);
  protected readonly availableDays = signal<ReadonlySet<string> | null>(null);
  protected readonly date = signal<Date | null>(null);
  protected readonly slots = signal<Slot[] | null>(null);
  protected readonly slot = signal<Slot | null>(null);
  protected readonly customer = signal<Customer | null>(null);
  protected readonly customerResults = signal<Customer[]>([]);
  protected readonly booked = signal<Booking | null>(null);
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  protected readonly step = computed<Step>(() => {
    if (!this.service()) {
      return 0;
    }
    if (!this.barber()) {
      return 1;
    }
    return this.slot() ? 3 : 2;
  });

  protected readonly slotGroups = computed(() => groupSlots(this.slots() ?? []));

  /** A naptár csak a szabad napokat engedi kiválasztani; új függvény, ha a napok változnak. */
  protected readonly dateFilter = computed(() => {
    const days = this.availableDays();
    return (date: Date | null) => date !== null && days !== null && days.has(toIsoDate(date));
  });

  /** A teljes választás az összegzéshez és a visszaigazoláshoz. */
  protected readonly selection = computed(() => {
    const service = this.service();
    const barber = this.barber();
    const date = this.date();
    const slot = this.slot();
    if (!service || !barber || !date || !slot) {
      return null;
    }
    return {
      service,
      barber,
      date,
      slot,
      end: addMinutesToTime(slot.time, service.durationMinutes),
    };
  });

  protected readonly serviceSummary = computed(() => {
    const service = this.service();
    if (!service) {
      return '';
    }
    const price = formatCurrency(service.priceHuf, this.locale, 'Ft', 'HUF', '1.0-0');
    return `${service.name} · ${service.durationMinutes} perc · ${price}`;
  });
  protected readonly barberSummary = computed(() => this.barber()?.fullName ?? '');
  protected readonly timeSummary = computed(() => {
    const selection = this.selection();
    if (!selection) {
      return '';
    }
    return `${formatDate(selection.date, 'fullDate', this.locale)}, ${selection.slot.time}–${selection.end}`;
  });

  protected readonly customerQuery = new FormControl<string | Customer>('', { nonNullable: true });
  protected readonly customerLabel = (value: Customer | string | null): string =>
    typeof value === 'string' ? value : value ? `${value.fullName} (${value.email})` : '';

  /** Címsorból visszaállítandó választás; az első saját kattintás eldobja. */
  private pending: PendingSelection | null;

  constructor() {
    this.pending = readPendingSelection(this.route.snapshot.queryParamMap);

    this.catalog
      .services()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (services) => {
          this.services.set(services);
          const wanted = services.find((service) => service.id === this.pending?.serviceId);
          if (wanted) {
            this.selectService(wanted, true);
          } else {
            this.pending = null;
          }
        },
        error: (error: unknown) => this.showError(error),
      });

    // Vendégkereső: gépelés közben 300 ms szünet után keres, legalább 2 karakterrel.
    this.customerQuery.valueChanges
      .pipe(
        map((value) => (typeof value === 'string' ? value.trim() : null)),
        tap((query) => {
          if (query !== null) {
            this.customer.set(null);
          }
        }),
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((query) =>
          query && query.length >= 2
            ? this.customers.search(query).pipe(catchError(() => of<Customer[]>([])))
            : of<Customer[]>([]),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((results) => this.customerResults.set(results));
  }

  protected stateOf(step: Step): StepState {
    const current = this.step();
    return step < current ? 'done' : step === current ? 'active' : 'todo';
  }

  protected selectService(service: ServiceOffering, restoring = false): void {
    this.startSelection(restoring);
    this.service.set(service);
    this.resetFrom(1);
    this.syncUrl();
    this.catalog
      .barbers(service.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (barbers) => {
          if (this.service() !== service) {
            return;
          }
          const visible = this.visibleBarbers(barbers);
          this.barbers.set(visible);
          const wanted =
            visible.find((barber) => barber.id === this.pending?.barberId) ??
            (this.auth.role() === 'BARBER' && visible.length === 1 ? visible[0] : undefined);
          if (wanted) {
            this.selectBarber(wanted, true);
          } else {
            this.pending = null;
          }
        },
        error: (error: unknown) => this.showError(error),
      });
  }

  protected selectBarber(barber: BarberPublic, restoring = false): void {
    const service = this.service();
    if (!service) {
      return;
    }
    this.startSelection(restoring);
    this.barber.set(barber);
    this.resetFrom(2);
    this.syncUrl();
    this.bookings
      .availableDays(barber.id, service.id, toIsoDate(this.today), toIsoDate(this.lastDay))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (days) => {
          if (this.barber() !== barber) {
            return;
          }
          const available = new Set(days);
          this.availableDays.set(available);
          const wanted = this.pending?.date;
          if (wanted && available.has(wanted)) {
            this.selectDate(fromIsoDate(wanted), true);
            return;
          }
          if (wanted) {
            this.notice.set('A korábban választott nap már nem foglalható, válassz másikat.');
            this.pending = null;
          }
          // A legkorábbi szabad nap legyen kiválasztva, hogy rögtön látszódjanak az időpontok.
          if (days.length > 0) {
            this.selectDate(fromIsoDate(days[0]), true);
          }
        },
        error: (error: unknown) => this.showError(error),
      });
  }

  protected selectDate(date: Date | null, restoring = false): void {
    const service = this.service();
    const barber = this.barber();
    if (!date || !service || !barber) {
      return;
    }
    this.startSelection(restoring);
    this.date.set(date);
    this.slots.set(null);
    this.slot.set(null);
    this.syncUrl();
    this.bookings
      .slots(barber.id, service.id, toIsoDate(date))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (slots) => {
          if (this.date() !== date) {
            return;
          }
          this.slots.set(slots);
          const wanted = this.pending?.startAt;
          if (wanted) {
            const match = slots.find((slot) => sameInstant(slot.startAt, wanted));
            if (match) {
              this.selectSlot(match, true);
            } else {
              this.notice.set('A korábban választott időpont közben elkelt, válassz másikat.');
            }
          }
          this.pending = null;
        },
        error: (error: unknown) => this.showError(error),
      });
  }

  protected selectSlot(slot: Slot, restoring = false): void {
    this.startSelection(restoring);
    this.slot.set(slot);
    this.syncUrl();
  }

  /** „Módosítás”: az adott lépéstől kezdve újra kell választani. */
  protected editStep(step: Step): void {
    this.startSelection(false);
    if (step === 0) {
      this.service.set(null);
      this.resetFrom(1);
    } else if (step === 1) {
      this.barber.set(null);
      this.resetFrom(2);
    } else {
      this.slot.set(null);
    }
    this.syncUrl();
  }

  protected selectCustomer(event: MatAutocompleteSelectedEvent): void {
    this.customer.set(event.option.value as Customer);
  }

  protected book(): void {
    const selection = this.selection();
    if (!selection) {
      return;
    }
    const customer = this.isStaff() ? this.customer() : null;
    if (this.isStaff() && !customer) {
      this.error.set('Válaszd ki, melyik vendég nevére szól a foglalás.');
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.bookings
      .create({
        barberId: selection.barber.id,
        serviceId: selection.service.id,
        startAt: selection.slot.startAt,
        ...(customer ? { customerId: customer.id } : {}),
      })
      .subscribe({
        next: (booking) => {
          this.submitting.set(false);
          this.booked.set(booking);
          void this.router.navigate([], {
            relativeTo: this.route,
            queryParams: {},
            replaceUrl: true,
          });
        },
        error: (error: unknown) => {
          this.submitting.set(false);
          const problem = toProblem(error);
          this.error.set(problem.message);
          if (problem.status === 409) {
            // Közben elkelt (vagy lejárt a foglalási határidő): friss időpontok ugyanarra a napra.
            this.selectDate(this.date(), true);
          }
        },
      });
  }

  protected newBooking(): void {
    this.booked.set(null);
    this.service.set(null);
    this.resetFrom(1);
    this.customer.set(null);
    this.customerQuery.setValue('');
    this.error.set(null);
    this.notice.set(null);
  }

  /** A belépés és a regisztráció ide tér vissza, a választással együtt. */
  protected currentUrl(): string {
    return this.router.url;
  }

  protected initials(name: string): string {
    return name
      .split(/\s+/)
      .filter((part) => part.length > 0)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
  }

  /** Saját kattintásnál eldobja a visszaállítást és a korábbi üzeneteket; visszaállításkor megtartja őket. */
  private startSelection(restoring: boolean): void {
    if (!restoring) {
      this.pending = null;
      this.notice.set(null);
      this.error.set(null);
    }
  }

  private resetFrom(step: 1 | 2): void {
    if (step === 1) {
      this.barbers.set(null);
      this.barber.set(null);
    }
    this.availableDays.set(null);
    this.date.set(null);
    this.slots.set(null);
    this.slot.set(null);
  }

  /** A borbély csak a saját naptárába vehet fel foglalást. */
  private visibleBarbers(barbers: BarberPublic[]): BarberPublic[] {
    const user = this.auth.user();
    return user?.role === 'BARBER'
      ? barbers.filter((barber) => barber.id === user.barberId)
      : barbers;
  }

  private syncUrl(): void {
    const date = this.date();
    void this.router.navigate([], {
      relativeTo: this.route,
      replaceUrl: true,
      queryParams: {
        serviceId: this.service()?.id ?? null,
        barberId: this.barber()?.id ?? null,
        date: date ? toIsoDate(date) : null,
        startAt: this.slot()?.startAt ?? null,
      },
    });
  }

  private showError(error: unknown): void {
    this.error.set(toProblem(error).message);
  }
}
