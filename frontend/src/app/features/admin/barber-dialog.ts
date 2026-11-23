import { ClipboardModule } from '@angular/cdk/clipboard';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable, map, of, switchMap } from 'rxjs';

import { BarberDetails, ServiceOffering } from '../../core/api/api.models';
import { BarberApi } from '../../core/api/barber-api';
import { applyFieldErrors, errorMessage } from '../../core/forms/form-errors';
import { PASSWORD_HINT, strongPassword } from '../../core/forms/validators';
import { toProblem } from '../../core/http/problem';
import { leadTimeLabel, leadTimeOptions } from '../../core/time/lead-time';
import { generateInitialPassword } from './initial-password';

export interface BarberDialogData {
  /** null: új borbély felvétele. */
  barber: BarberDetails | null;
  /** Az összes szolgáltatás, a kivezetettekkel együtt. */
  services: ServiceOffering[];
}

const NAME_MAX = 100;
const BIO_MAX = 500;
const EMAIL_MAX = 254;

/**
 * Borbély felvétele kezdőjelszóval, vagy adatainak módosítása (név, bemutatkozás, foglalási
 * határidő, vállalt szolgáltatások). A mentés az ablakban történik, hiba esetén nyitva marad.
 */
@Component({
  selector: 'app-barber-dialog',
  imports: [
    ClipboardModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
  ],
  templateUrl: './barber-dialog.html',
  styles: `
    .full {
      width: 100%;
    }

    mat-form-field {
      margin-bottom: 8px;
    }

    .services {
      display: flex;
      flex-direction: column;
      margin: 8px 0 0;
      padding: 0;
      border: 0;

      legend {
        margin-bottom: 4px;
        font: var(--mat-sys-title-small);
      }
    }
  `,
})
export class BarberDialog {
  private readonly api = inject(BarberApi);
  private readonly dialogRef = inject<MatDialogRef<BarberDialog, BarberDetails>>(MatDialogRef);
  protected readonly data = inject<BarberDialogData>(MAT_DIALOG_DATA);
  protected readonly barber = this.data.barber;

  protected readonly nameMax = NAME_MAX;
  protected readonly bioMax = BIO_MAX;
  protected readonly passwordHint = PASSWORD_HINT;
  protected readonly errorMessage = errorMessage;
  protected readonly leadTimeLabel = leadTimeLabel;
  protected readonly leadOptions = leadTimeOptions(this.barber?.minLeadMinutes);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  /** A választható szolgáltatások: az aktívak és a borbélynál már beállított kivezetettek. */
  protected readonly serviceChoices = this.data.services.filter(
    (service) =>
      service.active || this.barber?.services.some((own) => own.id === service.id) === true,
  );
  protected readonly selected = signal(
    new Set<number>(this.barber?.services.map((service) => service.id) ?? []),
  );

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(EMAIL_MAX)]],
    initialPassword: [generateInitialPassword(), strongPassword],
    fullName: [this.barber?.fullName ?? '', [Validators.required, Validators.maxLength(NAME_MAX)]],
    bio: [this.barber?.bio ?? '', Validators.maxLength(BIO_MAX)],
    minLeadMinutes: [this.barber?.minLeadMinutes ?? 60, Validators.required],
  });
  protected readonly messages = {
    email: { required: 'Add meg az e-mail-címet.', email: 'Érvénytelen e-mail-cím.' },
    fullName: { required: 'Add meg a nevet.' },
    bio: { maxlength: `Legfeljebb ${BIO_MAX} karakter lehet.` },
  };

  constructor() {
    // Módosításkor az e-mail-cím és a jelszó nem változik (a jelszót a borbély maga cseréli).
    if (this.barber) {
      this.form.controls.email.disable();
      this.form.controls.initialPassword.disable();
    }
  }

  protected newPassword(): void {
    this.form.controls.initialPassword.setValue(generateInitialPassword());
  }

  protected toggle(serviceId: number, checked: boolean): void {
    this.selected.update((current) => {
      const next = new Set(current);
      if (checked) {
        next.add(serviceId);
      } else {
        next.delete(serviceId);
      }
      return next;
    });
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const bio = value.bio.trim() === '' ? null : value.bio.trim();
    const serviceIds = [...this.selected()];
    const barber = this.barber;
    const call: Observable<BarberDetails> = barber
      ? this.api
          .update(barber.id, {
            fullName: value.fullName.trim(),
            bio,
            minLeadMinutes: value.minLeadMinutes,
            active: barber.active,
          })
          .pipe(
            switchMap((updated) =>
              this.servicesChanged(barber, serviceIds)
                ? this.api.replaceServices(barber.id, serviceIds)
                : of(updated),
            ),
          )
      : this.api.create({
          email: value.email.trim(),
          fullName: value.fullName.trim(),
          initialPassword: value.initialPassword,
          bio,
          minLeadMinutes: value.minLeadMinutes,
          serviceIds,
        });
    this.saving.set(true);
    this.error.set(null);
    call.subscribe({
      next: (saved) => this.dialogRef.close(saved),
      error: (error: unknown) => {
        const problem = toProblem(error);
        const unmatched = applyFieldErrors(this.form, problem.fieldErrors);
        this.saving.set(false);
        // Foglalt e-mail-cím (409) felvételkor: a mezőhöz tartozik.
        if (problem.status === 409 && !barber) {
          this.form.controls.email.setErrors({ server: problem.message });
          this.form.controls.email.markAsTouched();
          return;
        }
        this.error.set(unmatched.length > 0 ? unmatched.join(' ') : problem.message);
      },
    });
  }

  private servicesChanged(barber: BarberDetails, serviceIds: number[]): boolean {
    const before = new Set(barber.services.map((service) => service.id));
    return before.size !== serviceIds.length || serviceIds.some((id) => !before.has(id));
  }
}

/** Megnyitja az ablakot; a mentett borbély, vagy undefined, ha mégsem mentettek. */
export function openBarberDialog(
  dialog: MatDialog,
  data: BarberDialogData,
): Observable<BarberDetails | undefined> {
  return (
    dialog
      .open<BarberDialog, BarberDialogData, BarberDetails>(BarberDialog, {
        data,
        width: '560px',
      })
      .afterClosed()
      // A „Mégse” (sima mat-dialog-close) üres szöveggel zár.
      .pipe(map((result) => (result && typeof result === 'object' ? result : undefined)))
  );
}
