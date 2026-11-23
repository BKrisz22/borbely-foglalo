import { Component, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { afterLoginUrl } from '../../core/auth/return-url';
import { applyFieldErrors, errorMessage } from '../../core/forms/form-errors';
import { PASSWORD_HINT, sameAs, strongPassword } from '../../core/forms/validators';
import { toProblem } from '../../core/http/problem';

/** Vendégregisztráció; sikeres regisztráció után a felhasználó már be is van lépve. */
@Component({
  selector: 'app-register-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './register-page.html',
  styleUrl: './auth-page.scss',
})
export class RegisterPage {
  readonly returnUrl = input<string>();

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group({
    fullName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', strongPassword],
    confirmPassword: ['', [Validators.required, sameAs('password', 'A két jelszó nem egyezik.')]],
  });
  protected readonly messages = {
    fullName: { required: 'Add meg a neved.', maxlength: 'A név legfeljebb 100 karakter lehet.' },
    email: {
      required: 'Add meg az e-mail-címed.',
      email: 'Érvénytelen e-mail-cím.',
      maxlength: 'Az e-mail-cím legfeljebb 254 karakter lehet.',
    },
    confirmPassword: { required: 'Írd be újra a jelszót.' },
  };
  protected readonly passwordHint = PASSWORD_HINT;
  protected readonly errorMessage = errorMessage;
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    // Ha a jelszó változik, a megerősítést újra kell ellenőrizni.
    this.form.controls.password.valueChanges
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
    const { fullName, email, password } = this.form.getRawValue();
    this.submitting.set(true);
    this.error.set(null);
    this.auth.register({ fullName: fullName.trim(), email: email.trim(), password }).subscribe({
      next: (user) => {
        const target = afterLoginUrl(user, this.returnUrl(), this.auth.homeUrl());
        void this.router.navigateByUrl(target).finally(() => this.submitting.set(false));
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
