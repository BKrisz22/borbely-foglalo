import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Observable, map } from 'rxjs';

import { ServiceOffering, ServiceOfferingRequest } from '../../core/api/api.models';
import { ServiceOfferingApi } from '../../core/api/service-offering-api';
import { applyFieldErrors, errorMessage } from '../../core/forms/form-errors';
import { toProblem } from '../../core/http/problem';

const NAME_MAX = 100;
const DESCRIPTION_MAX = 500;
const DURATION_MAX = 480;

/** Szolgáltatás felvétele vagy módosítása; a mentés az ablakban történik, hiba esetén nyitva marad. */
@Component({
  selector: 'app-service-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ service ? 'Szolgáltatás módosítása' : 'Új szolgáltatás' }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()" novalidate>
      <mat-dialog-content>
        @if (error(); as message) {
          <div class="form-error" role="alert">
            <mat-icon>error</mat-icon>
            <span>{{ message }}</span>
          </div>
        }
        <mat-form-field class="full">
          <mat-label>Név</mat-label>
          <input matInput formControlName="name" [maxlength]="nameMax" cdkFocusInitial />
          <mat-error>{{ errorMessage(form.controls.name, messages.name) }}</mat-error>
        </mat-form-field>
        <mat-form-field class="full">
          <mat-label>Leírás (nem kötelező)</mat-label>
          <textarea
            matInput
            formControlName="description"
            rows="2"
            [maxlength]="descriptionMax"
          ></textarea>
          <mat-error>{{ errorMessage(form.controls.description) }}</mat-error>
        </mat-form-field>
        <div class="pair">
          <mat-form-field subscriptSizing="dynamic">
            <mat-label>Időtartam</mat-label>
            <input
              matInput
              type="number"
              formControlName="durationMinutes"
              min="5"
              [max]="durationMax"
              step="5"
            />
            <span matTextSuffix>perc</span>
            <mat-hint>A naptár negyedórás rácsot használ (15, 30, 45…).</mat-hint>
            <mat-error>{{
              errorMessage(form.controls.durationMinutes, messages.durationMinutes)
            }}</mat-error>
          </mat-form-field>
          <mat-form-field subscriptSizing="dynamic">
            <mat-label>Ár</mat-label>
            <input matInput type="number" formControlName="priceHuf" min="0" step="100" />
            <span matTextSuffix>Ft</span>
            <mat-error>{{ errorMessage(form.controls.priceHuf, messages.priceHuf) }}</mat-error>
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button matButton type="button" mat-dialog-close>Mégse</button>
        <button matButton="filled" type="submit" [disabled]="saving()">
          {{ saving() ? 'Mentés…' : 'Mentés' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .full {
      width: 100%;
    }

    .pair {
      display: grid;
      grid-template-columns: 1fr 1fr;
      align-items: start;
      gap: 16px;
    }

    @media (max-width: 480px) {
      .pair {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class ServiceDialog {
  private readonly api = inject(ServiceOfferingApi);
  private readonly dialogRef = inject<MatDialogRef<ServiceDialog, ServiceOffering>>(MatDialogRef);
  protected readonly service = inject<ServiceOffering | null>(MAT_DIALOG_DATA);

  protected readonly nameMax = NAME_MAX;
  protected readonly descriptionMax = DESCRIPTION_MAX;
  protected readonly durationMax = DURATION_MAX;
  protected readonly errorMessage = errorMessage;
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    name: [this.service?.name ?? '', [Validators.required, Validators.maxLength(NAME_MAX)]],
    description: [this.service?.description ?? '', Validators.maxLength(DESCRIPTION_MAX)],
    durationMinutes: [
      this.service?.durationMinutes ?? 30,
      [Validators.required, Validators.min(1), Validators.max(DURATION_MAX)],
    ],
    priceHuf: [this.service?.priceHuf ?? 0, [Validators.required, Validators.min(0)]],
  });
  protected readonly messages = {
    name: { required: 'Add meg a szolgáltatás nevét.' },
    durationMinutes: {
      required: 'Add meg az időtartamot.',
      min: 'Legalább 1 perc.',
      max: `Legfeljebb ${DURATION_MAX} perc.`,
    },
    priceHuf: { required: 'Add meg az árat.', min: 'Az ár nem lehet negatív.' },
  };

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const request: ServiceOfferingRequest = {
      name: value.name.trim(),
      description: value.description.trim() === '' ? null : value.description.trim(),
      durationMinutes: value.durationMinutes,
      priceHuf: value.priceHuf,
      active: this.service?.active ?? true,
    };
    this.saving.set(true);
    this.error.set(null);
    const call = this.service
      ? this.api.update(this.service.id, request)
      : this.api.create(request);
    call.subscribe({
      next: (saved) => this.dialogRef.close(saved),
      error: (error: unknown) => {
        const problem = toProblem(error);
        const unmatched = applyFieldErrors(this.form, problem.fieldErrors);
        this.saving.set(false);
        // A névütközés (409) a névhez tartozik.
        if (problem.status === 409) {
          this.form.controls.name.setErrors({ server: problem.message });
          this.form.controls.name.markAsTouched();
          return;
        }
        this.error.set(unmatched.length > 0 ? unmatched.join(' ') : problem.message);
      },
    });
  }
}

/** Megnyitja az ablakot; a mentett szolgáltatás, vagy undefined, ha mégsem mentettek. */
export function openServiceDialog(
  dialog: MatDialog,
  service: ServiceOffering | null,
): Observable<ServiceOffering | undefined> {
  return (
    dialog
      .open<ServiceDialog, ServiceOffering | null, ServiceOffering>(ServiceDialog, {
        data: service,
        width: '520px',
      })
      .afterClosed()
      // A „Mégse” (sima mat-dialog-close) üres szöveggel zár.
      .pipe(map((result) => (result && typeof result === 'object' ? result : undefined)))
  );
}
