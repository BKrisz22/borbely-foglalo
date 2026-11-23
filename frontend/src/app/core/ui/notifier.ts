import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/** Rövid visszajelzések a képernyő alján (Material snackbar). */
@Injectable({ providedIn: 'root' })
export class Notifier {
  private readonly snackBar = inject(MatSnackBar);

  info(message: string): void {
    this.snackBar.open(message, 'OK', { duration: 4000 });
  }

  error(message: string): void {
    this.snackBar.open(message, 'OK', { duration: 8000, panelClass: 'snack-error' });
  }
}
