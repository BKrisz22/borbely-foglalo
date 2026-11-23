import { Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { Observable, map } from 'rxjs';

export interface NewEntryData {
  /** A kijelölt időszak olvasható alakban (rangeLabel). */
  label: string;
  /** Munkaidő csak egy napon belül vehető fel. */
  sameDay: boolean;
}

export type NewEntryResult = { kind: 'availability' } | { kind: 'timeOff'; reason: string | null };

const REASON_MAX = 200;

/** A beosztásban kijelölt időszakból munkaidő vagy távollét lesz. */
@Component({
  selector: 'app-new-entry-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
  ],
  template: `
    <h2 mat-dialog-title>Új bejegyzés</h2>
    <mat-dialog-content>
      <p class="range">{{ data.label }}</p>
      <mat-radio-group
        class="kinds"
        aria-label="Bejegyzés fajtája"
        [value]="kind()"
        (change)="kind.set($event.value)"
      >
        <mat-radio-button value="availability" [disabled]="!data.sameDay"
          >Munkaidő</mat-radio-button
        >
        <mat-radio-button value="timeOff"
          >Távollét (szabadság, betegség, ügyintézés)</mat-radio-button
        >
      </mat-radio-group>
      @if (!data.sameDay) {
        <p class="muted">
          Munkaidő csak egy napon belül vehető fel; több napra távollét adható meg.
        </p>
      }
      @if (kind() === 'timeOff') {
        <mat-form-field appearance="outline" class="reason">
          <mat-label>Indoklás (nem kötelező)</mat-label>
          <input matInput [formControl]="reason" [maxlength]="reasonMax" />
          <mat-hint align="end">{{ reason.value.length }} / {{ reasonMax }}</mat-hint>
        </mat-form-field>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton type="button" mat-dialog-close>Mégse</button>
      <button matButton="filled" type="button" (click)="save()">Mentés</button>
    </mat-dialog-actions>
  `,
  styles: `
    .range {
      margin: 0 0 16px;
      font: var(--mat-sys-title-medium);
    }

    .kinds {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .muted {
      margin: 8px 0 0;
    }

    .reason {
      width: 100%;
      margin-top: 16px;
    }
  `,
})
export class NewEntryDialog {
  protected readonly data = inject<NewEntryData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject<MatDialogRef<NewEntryDialog, NewEntryResult>>(MatDialogRef);

  protected readonly reasonMax = REASON_MAX;
  protected readonly kind = signal<NewEntryResult['kind']>(
    this.data.sameDay ? 'availability' : 'timeOff',
  );
  protected readonly reason = new FormControl('', {
    nonNullable: true,
    validators: [Validators.maxLength(REASON_MAX)],
  });

  protected save(): void {
    if (this.kind() === 'availability') {
      this.dialogRef.close({ kind: 'availability' });
      return;
    }
    const reason = this.reason.value.trim();
    this.dialogRef.close({ kind: 'timeOff', reason: reason === '' ? null : reason });
  }
}

/** Megnyitja az ablakot; undefined, ha a felhasználó mégsem vett fel semmit. */
export function openNewEntry(
  dialog: MatDialog,
  data: NewEntryData,
): Observable<NewEntryResult | undefined> {
  return (
    dialog
      .open<NewEntryDialog, NewEntryData, NewEntryResult>(NewEntryDialog, { data, width: '440px' })
      .afterClosed()
      // A „Mégse” (sima mat-dialog-close) üres szöveggel zár, nem undefineddal.
      .pipe(map((result) => (result && typeof result === 'object' ? result : undefined)))
  );
}
