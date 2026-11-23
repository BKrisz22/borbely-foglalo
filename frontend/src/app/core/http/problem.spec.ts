import { HttpErrorResponse } from '@angular/common/http';

import { toProblem } from './problem';

describe('toProblem', () => {
  it('a ProblemDetail detail mezőjét adja üzenetnek', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { title: 'Ütközés', detail: 'Ez az időpont már foglalt.', status: 409 },
    });
    expect(toProblem(error)).toEqual({
      status: 409,
      title: 'Ütközés',
      message: 'Ez az időpont már foglalt.',
      fieldErrors: {},
    });
  });

  it('a mezőhibákat is kiolvassa', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        title: 'Érvénytelen adatok',
        detail: 'Egy vagy több mező hibás.',
        errors: {
          email: 'Érvénytelen e-mail-cím.',
          password: 'A jelszó legalább 8 karakter legyen.',
        },
      },
    });
    expect(toProblem(error).fieldErrors).toEqual({
      email: 'Érvénytelen e-mail-cím.',
      password: 'A jelszó legalább 8 karakter legyen.',
    });
  });

  it('kapcsolati hibánál (0) a backend elérhetetlenségére utal', () => {
    const problem = toProblem(new HttpErrorResponse({ status: 0 }));
    expect(problem.status).toBe(0);
    expect(problem.message).toContain('backend');
  });

  it('a fejlesztői proxy üres 5xx válaszát is kapcsolati hibának veszi', () => {
    const problem = toProblem(new HttpErrorResponse({ status: 504, error: null }));
    expect(problem.message).toContain('backend');
    expect(problem.message).toContain('504');
  });

  it('törzs nélküli válasznál a státusz szerinti alapüzenetet adja', () => {
    expect(toProblem(new HttpErrorResponse({ status: 404 })).message).toBe(
      'A keresett adat nem található.',
    );
    expect(toProblem(new HttpErrorResponse({ status: 403 })).message).toBe(
      'Nincs jogosultságod ehhez a művelethez.',
    );
  });

  it('nem HTTP-hibánál általános üzenetet ad', () => {
    expect(toProblem(new Error('valami')).status).toBe(-1);
  });
});
