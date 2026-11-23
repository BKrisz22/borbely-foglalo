import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import localeHu from '@angular/common/locales/hu';
import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import {
  MAT_FORM_FIELD_DEFAULT_OPTIONS,
  MatFormFieldDefaultOptions,
} from '@angular/material/form-field';
import { TitleStrategy, provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { AuthService } from './core/auth/auth.service';
import { AppTitleStrategy } from './core/ui/app-title-strategy';

// Magyar dátum-, szám- és pénznemformátum (például „6 500 Ft”, „2026. december 1.”).
registerLocaleData(localeHu);

const formFieldDefaults: MatFormFieldDefaultOptions = { appearance: 'outline' };

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // withComponentInputBinding: az útvonal paraméterei, a query és a data a komponens inputjaiba kerül.
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
    { provide: LOCALE_ID, useValue: 'hu' },
    { provide: TitleStrategy, useClass: AppTitleStrategy },
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: formFieldDefaults },
    provideAppInitializer(() => inject(AuthService).syncUser()),
  ],
};
