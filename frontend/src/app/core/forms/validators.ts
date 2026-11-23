import { ValidatorFn } from '@angular/forms';

/**
 * A jelszószabályok a backend StrongPasswordValidator-ával egyeznek, hogy a hiba már
 * beküldés előtt látszódjon. A végső ellenőrzés a szerveren történik.
 */
export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 64;
/** A BCrypt csak az első 72 bájtot veszi figyelembe. */
export const PASSWORD_MAX_BYTES = 72;

export const PASSWORD_HINT = `Legalább ${PASSWORD_MIN_LENGTH} karakter, betűvel és számjeggyel.`;

/** A jelszó hibája szövegesen, vagy null, ha megfelel. */
export function passwordProblem(value: string | null | undefined): string | null {
  if (!value || value.trim() === '') {
    return 'A jelszó megadása kötelező.';
  }
  if (value.length < PASSWORD_MIN_LENGTH) {
    return `A jelszó legalább ${PASSWORD_MIN_LENGTH} karakter legyen.`;
  }
  if (
    value.length > PASSWORD_MAX_LENGTH ||
    new TextEncoder().encode(value).length > PASSWORD_MAX_BYTES
  ) {
    return `A jelszó túl hosszú: legfeljebb ${PASSWORD_MAX_LENGTH} karakter lehet, ékezetes betűkkel kevesebb.`;
  }
  if (!/\p{L}/u.test(value) || !/\p{Nd}/u.test(value)) {
    return 'A jelszóban legyen legalább egy betű és egy számjegy.';
  }
  return null;
}

/** Erős jelszó; hiba esetén { password: 'üzenet' }. */
export const strongPassword: ValidatorFn = (control) => {
  const problem = passwordProblem(control.value as string | null);
  return problem ? { password: problem } : null;
};

/**
 * Egyezzen a szülőcsoport megadott mezőjével (jelszó megerősítése). A másik mező
 * változásakor ezt a mezőt újra kell validálni (updateValueAndValidity).
 */
export function sameAs(field: string, message: string): ValidatorFn {
  return (control) => {
    const other = control.parent?.get(field);
    if (!other || control.value === other.value) {
      return null;
    }
    return { sameAs: message };
  };
}
