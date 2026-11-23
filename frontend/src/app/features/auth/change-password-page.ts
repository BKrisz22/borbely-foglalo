import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { applyFieldErrors, errorMessage } from '../../core/forms/form-errors';
import { PASSWORD_HINT, sameAs, strongPassword } from '../../core/forms/validators';
import { toProblem } from '../../core/http/problem';
import { Notifier } from '../../core/ui/notifier';

/**
 * Jelszócsere. A tulajdonos által felvett borbélyoknak az első belépéskor kötelező
 * (mustChangePassword); addig a guardok minden védett oldalról ide irányítanak.
 */
@Component({
  selector: 'app-change-password-page',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './change-password-page.html',
  styleUrl: './auth-page.scss',
})
export class ChangePasswordPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifier = inject(Notifier);

  protected readonly mustChange = computed(() => this.auth.user()?.mustChangePassword ?? false);
  protected readonly form = inject(NonNullableFormBuilder).group({
    currentPassword: ['', Validators.required],
    newPassword: ['', strongPassword],
    confirmPassword: [
      '',
      [Validators.required, sameAs('newPassword', 'A két jelszó nem egyezik.')],
    ],
  });
  protected readonly messages = {
    currentPassword: { required: 'Add meg a jelenlegi jelszavad.' },
    confirmPassword: { required: 'Írd be újra az új jelszót.' },
  };
  protected readonly passwordHint = PASSWORD_HINT;
  protected readonly errorMessage = errorMessage;
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.form.controls.newPassword.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() =>
        this.form.controls.confirmPassword.updateValueAndValidity({ emitEvent: false }),
      );
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { currentPassword, newPassword } = this.form.getRawValue();
    this.submitting.set(true);
    this.error.set(null);
    this.auth.changePassword({ currentPassword, newPassword }).subscribe({
      next: () => {
        this.notifier.info('A jelszavad megváltozott.');
        void this.router
          .navigateByUrl(this.auth.homeUrl())
          .finally(() => this.submitting.set(false));
      },
      error: (error: unknown) => {
        const problem = toProblem(error);
        const unmatched = applyFieldErrors(this.form, problem.fieldErrors);
        this.submitting.set(false);
        this.error.set(unmatched.length > 0 ? unmatched.join(' ') : problem.message);
      },
    });
  }
}
