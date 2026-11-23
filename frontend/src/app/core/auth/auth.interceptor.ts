import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth.service';

/**
 * Az /api/ kérésekhez hozzáteszi a Bearer tokent. Ha a szerver egy tokennel küldött kérést
 * 401-gyel utasít el (lejárt, hamis vagy a szerver kulcsa megváltozott), kilépteti a felhasználót.
 * Token nélküli 401 (például hibás jelszó a belépésnél) nem léptet ki senkit.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api/')) {
    return next(request);
  }
  const auth = inject(AuthService);
  const token = auth.token();
  const authorized = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;
  return next(authorized).pipe(
    catchError((error: unknown) => {
      if (token && error instanceof HttpErrorResponse && error.status === 401) {
        auth.sessionExpired();
      }
      return throwError(() => error);
    }),
  );
};
