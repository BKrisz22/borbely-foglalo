import { Component, inject } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';

import { StaffBarberContext } from './staff-barber';

/** Borbélyválasztó a tulajdonosnak; a borbély a saját naptárát látja, neki nem jelenik meg. */
@Component({
  selector: 'app-barber-picker',
  imports: [MatFormFieldModule, MatSelectModule],
  template: `
    @if (context.isOwner()) {
      <mat-form-field appearance="outline" subscriptSizing="dynamic" class="picker">
        <mat-label>Borbély</mat-label>
        <mat-select [value]="context.barberId()" (valueChange)="context.select($event)">
          @for (barber of context.barbers() ?? []; track barber.id) {
            <mat-option [value]="barber.id">{{ barber.fullName }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
    }
  `,
  styles: `
    .picker {
      width: 220px;
    }
  `,
})
export class BarberPicker {
  protected readonly context = inject(StaffBarberContext);

  constructor() {
    this.context.load();
  }
}
