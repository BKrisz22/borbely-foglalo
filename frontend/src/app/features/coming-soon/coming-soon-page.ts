import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

/**
 * Helyőrző egy még el nem készült oldalhoz. Az adatai az útvonal data mezőjéből jönnek
 * (withComponentInputBinding), lásd comingSoon() az app.routes.ts-ben.
 */
@Component({
  selector: 'app-coming-soon-page',
  imports: [MatCardModule, MatIconModule],
  templateUrl: './coming-soon-page.html',
  styleUrl: './coming-soon-page.scss',
})
export class ComingSoonPage {
  readonly heading = input('');
  readonly icon = input('construction');
  readonly plans = input<string[]>([]);
  /** A bemutató utáni bővítések része. */
  readonly extra = input(false);
}
