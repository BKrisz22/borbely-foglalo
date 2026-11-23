import { AbstractControl } from '@angular/forms';

/**
 * Egy mező első hibájának szövege. Ha a hibakulcshoz van saját üzenet, azt adja vissza;
 * ha a hiba értéke szöveg (saját validátorok, szerver), azt; különben egy általános üzenetet.
 */
export function errorMessage(
  control: AbstractControl,
  messages: Readonly<Record<string, string>> = {},
): string {
  const errors = control.errors;
  if (!errors) {
    return '';
  }
  for (const key of Object.keys(errors)) {
    const custom = messages[key];
    if (custom) {
      return custom;
    }
    const value: unknown = errors[key];
    if (typeof value === 'string') {
      return value;
    }
  }
  return 'Hibás érték.';
}

/**
 * A szerver mezőhibáit (ProblemDetail errors) a megfelelő űrlapmezőkre teszi { server: üzenet }
 * alakban. A mező következő módosításakor a hiba magától eltűnik, mert a validátorok újrafutnak.
 *
 * @returns azok az üzenetek, amelyekhez nincs ilyen nevű mező
 */
export function applyFieldErrors(form: AbstractControl, errors: Record<string, string>): string[] {
  const unmatched: string[] = [];
  for (const [field, message] of Object.entries(errors)) {
    const control = form.get(field);
    if (control) {
      control.setErrors({ ...(control.errors ?? {}), server: message });
      control.markAsTouched();
    } else {
      unmatched.push(message);
    }
  }
  return unmatched;
}
