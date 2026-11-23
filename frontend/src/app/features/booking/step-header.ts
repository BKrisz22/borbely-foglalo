import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export type StepState = 'done' | 'active' | 'todo';

/** A tölcsér egy lépésének fejléce: sorszám vagy pipa, cím, a választás összefoglalója, módosítás. */
@Component({
  selector: 'app-step-header',
  imports: [MatButtonModule, MatIconModule],
  template: `
    <div class="header" [class.active]="state() === 'active'" [class.todo]="state() === 'todo'">
      <span class="badge" aria-hidden="true">
        @if (state() === 'done') {
          <mat-icon>check</mat-icon>
        } @else {
          {{ index() + 1 }}
        }
      </span>
      <div class="text">
        <h2 class="title">{{ title() }}</h2>
        @if (state() === 'done' && summary()) {
          <span class="summary">{{ summary() }}</span>
        }
      </div>
      @if (state() === 'done' && editable()) {
        <button matButton type="button" (click)="edit.emit()">Módosítás</button>
      }
    </div>
  `,
  styles: `
    .header {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
    }
    .badge {
      display: grid;
      flex: none;
      place-items: center;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: var(--mat-sys-primary);
      color: var(--mat-sys-on-primary);
      font: var(--mat-sys-label-large);
    }
    .badge mat-icon {
      width: 20px;
      height: 20px;
      font-size: 20px;
    }
    .todo .badge {
      background: var(--mat-sys-surface-container-highest);
      color: var(--mat-sys-on-surface-variant);
    }
    .text {
      display: flex;
      flex: 1 1 auto;
      flex-direction: column;
      min-width: 0;
    }
    .title {
      margin: 0;
      font: var(--mat-sys-title-medium);
    }
    .todo .title {
      color: var(--mat-sys-on-surface-variant);
    }
    .summary {
      color: var(--mat-sys-on-surface-variant);
    }
  `,
})
export class StepHeader {
  readonly index = input.required<number>();
  readonly title = input.required<string>();
  readonly state = input<StepState>('todo');
  readonly summary = input('');
  readonly editable = input(true);
  readonly edit = output<void>();
}
