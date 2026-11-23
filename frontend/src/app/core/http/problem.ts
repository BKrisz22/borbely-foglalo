import { HttpErrorResponse } from '@angular/common/http';

/**
 * Egy sikertelen kérés emberi nyelvű leírása. A backend hibái RFC 9457 ProblemDetail
 * formátumúak (GlobalExceptionHandler): title, detail, érvénytelen mezőknél errors: { mező: üzenet }.
 */
export interface Problem {
  /** HTTP-státusz; 0 = nincs kapcsolat, -1 = nem HTTP-hiba. */
  status: number;
  title: string;
  /** A felhasználónak megjeleníthető üzenet. */
  message: string;
  /** Mezőnév → hibaüzenet (400 Érvénytelen adatok esetén). */
  fieldErrors: Record<string, string>;
}

const NO_CONNECTION = 'A szerver nem érhető el. Ellenőrizd, hogy fut-e a backend.';

export function toProblem(error: unknown): Problem {
  if (!(error instanceof HttpErrorResponse)) {
    return problem(-1, 'Váratlan hiba', 'Váratlan hiba történt. Próbáld újra.');
  }
  if (error.status === 0) {
    return problem(0, 'Nincs kapcsolat', NO_CONNECTION);
  }
  const body: unknown = error.error;
  const title = readString(body, 'title');
  const detail = readString(body, 'detail');
  // A fejlesztői proxy üres törzsű 5xx-et ad, ha a backend nem fut.
  if (error.status >= 500 && !title && !detail) {
    return problem(error.status, 'Szerverhiba', `${NO_CONNECTION} (HTTP ${error.status})`);
  }
  return {
    status: error.status,
    title: title ?? defaultTitle(error.status),
    message: detail ?? title ?? defaultMessage(error.status),
    fieldErrors: readFieldErrors(body),
  };
}

function problem(status: number, title: string, message: string): Problem {
  return { status, title, message, fieldErrors: {} };
}

function asRecord(body: unknown): Record<string, unknown> | null {
  return body !== null && typeof body === 'object' ? (body as Record<string, unknown>) : null;
}

function readString(body: unknown, key: string): string | undefined {
  const value = asRecord(body)?.[key];
  return typeof value === 'string' && value.trim() !== '' ? value : undefined;
}

function readFieldErrors(body: unknown): Record<string, string> {
  const errors = asRecord(asRecord(body)?.['errors']);
  const result: Record<string, string> = {};
  if (errors) {
    for (const [field, message] of Object.entries(errors)) {
      if (typeof message === 'string') {
        result[field] = message;
      }
    }
  }
  return result;
}

function defaultTitle(status: number): string {
  if (status >= 500) {
    return 'Szerverhiba';
  }
  return status === 409 ? 'Ütközés' : 'Hiba';
}

function defaultMessage(status: number): string {
  if (status >= 500) {
    return 'Hiba történt a szerveren. Próbáld újra később.';
  }
  switch (status) {
    case 400:
      return 'Hibás kérés.';
    case 401:
      return 'Ehhez be kell jelentkezned.';
    case 403:
      return 'Nincs jogosultságod ehhez a művelethez.';
    case 404:
      return 'A keresett adat nem található.';
    case 409:
      return 'A művelet ütközik a meglévő adatokkal.';
    default:
      return 'Váratlan hiba történt.';
  }
}
