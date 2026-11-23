import { Component, inject, input, isDevMode, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { afterLoginUrl } from '../../core/auth/return-url';
import { errorMessage } from '../../core/forms/form-errors';
import { toProblem } from '../../core/http/problem';

interface DemoAccount {
  label: string;
  email: string;
  password: string;
}

/**
 * A bemutató adatok fiókjai (backend demo profil, DemoDataInitializer). Csak fejlesztői
 * módban (ng serve) jelennek meg, egy kattintással kitöltik az űrlapot.
 */
const DEMO_ACCOUNTS: readonly DemoAccount[] = [
  { label: 'Vendég (Vilma)', email: 'vilma@example.com', password: 'Demo1234' },
  { label: 'Borbély (Péter)', email: 'kovacs.peter@example.com', password: 'Demo1234' },
];

@Component({
  selector: 'app-login-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './login-page.html',
  styleUrl: './auth-page.scss',
})
export class LoginPage {
  /** Ahová belépés után vissza kell térni (?returnUrl=…, a guardok állítják be). */
  readonly returnUrl = input<string>();

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  protected readonly messages = {
    email: { required: 'Add meg az e-mail-címed.', email: 'Érvénytelen e-mail-cím.' },
    password: { required: 'Add meg a jelszavad.' },
  };
  protected readonly errorMessage = errorMessage;
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly hidePassword = signal(true);
  protected readonly demoAccounts = isDevMode() ? DEMO_ACCOUNTS : [];

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: (user) => {
        const target = afterLoginUrl(user, this.returnUrl(), this.auth.homeUrl());
        void this.router.navigateByUrl(target).finally(() => this.submitting.set(false));
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.error.set(toProblem(error).message);
      },
    });
  }

  protected fillDemo(account: DemoAccount): void {
    this.form.setValue({ email: account.email, password: account.password });
    this.error.set(null);
  }
}
