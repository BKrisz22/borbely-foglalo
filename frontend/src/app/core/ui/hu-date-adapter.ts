import { Injectable, Provider } from '@angular/core';
import {
  DateAdapter,
  MAT_DATE_FORMATS,
  MAT_DATE_LOCALE,
  MAT_NATIVE_DATE_FORMATS,
  NativeDateAdapter,
} from '@angular/material/core';

/** A Material naptár magyarul: hónap- és napnevek a böngésző Intl-adataiból, a hét hétfővel kezdődik. */
@Injectable()
export class HuDateAdapter extends NativeDateAdapter {
  override getFirstDayOfWeek(): number {
    return 1;
  }
}

export function provideHuDateAdapter(): Provider[] {
  return [
    { provide: MAT_DATE_LOCALE, useValue: 'hu-HU' },
    { provide: DateAdapter, useClass: HuDateAdapter },
    { provide: MAT_DATE_FORMATS, useValue: MAT_NATIVE_DATE_FORMATS },
  ];
}
