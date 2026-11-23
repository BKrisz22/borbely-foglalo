import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { BarberDetails } from '../../core/api/api.models';
import { BarberApi } from '../../core/api/barber-api';
import { AuthService } from '../../core/auth/auth.service';
import { applyFieldErrors, errorMessage } from '../../core/forms/form-errors';
import { toProblem } from '../../core/http/problem';
import { leadTimeLabel, leadTimeOptions } from '../../core/time/lead-time';
import { Notifier } from '../../core/ui/notifier';

const BIO_MAX = 500;

/**
 * A borbély saját profilja: a bemutatkozás (ez látszik a foglalásnál) és a foglalási határidő.
 * A nevet és a vállalt szolgáltatásokat a tulajdonos állítja be, itt csak látszanak.
 */
@Component({
  selector: 'app-profile-page',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
})
export class ProfilePage {
  private readonly api = inject(BarberApi);
  private readonly auth = inject(AuthService);
  private readonly notifier = inject(Notifier);

  protected readonly bioMax = BIO_MAX;
  protected readonly leadTimeLabel = leadTimeLabel;
  protected readonly errorMessage = errorMessage;

  protected readonly barber = signal<BarberDetails | null>(null);
  protected readonly loadError = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly leadOptions = signal<number[]>(leadTimeOptions());
  protected readonly serviceNames = computed(
    () =>
      this.barber()
        ?.services.map((service) => service.name)
        .join(', ') ?? '',
  );

  protected readonly form = inject(NonNullableFormBuilder).group({
    bio: ['', Validators.maxLength(BIO_MAX)],
    minLeadMinutes: [60, Validators.required],
  });
  protected readonly messages = {
    bio: { maxlength: `Legfeljebb ${BIO_MAX} karakter lehet.` },
  };

  constructor() {
    const barberId = this.auth.user()?.barberId;
    if (barberId === null || barberId === undefined) {
      this.loadError.set('Ehhez a fiókhoz nem tartozik borbélyprofil.');
      return;
    }
    this.api
      .get(barberId)
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (barber) => this.fill(barber),
        error: (error: unknown) => this.loadError.set(toProblem(error).message),
      });
  }

  protected save(): void {
    const barber = this.barber();
    if (!barber) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { bio, minLeadMinutes } = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.api
      .updateProfile(barber.id, { bio: bio.trim() === '' ? null : bio.trim(), minLeadMinutes })
      .subscribe({
        next: (updated) => {
          this.saving.set(false);
          this.fill(updated);
          this.notifier.info('A profilod mentve.');
        },
        error: (error: unknown) => {
          const problem = toProblem(error);
          const unmatched = applyFieldErrors(this.form, problem.fieldErrors);
          this.saving.set(false);
          this.error.set(unmatched.length > 0 ? unmatched.join(' ') : problem.message);
        },
      });
  }

  private fill(barber: BarberDetails): void {
    this.barber.set(barber);
    this.leadOptions.set(leadTimeOptions(barber.minLeadMinutes));
    this.form.reset({ bio: barber.bio ?? '', minLeadMinutes: barber.minLeadMinutes });
  }
}
