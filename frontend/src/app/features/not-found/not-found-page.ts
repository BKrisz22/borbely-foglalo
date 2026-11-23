import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found-page',
  imports: [MatButtonModule, RouterLink],
  template: `
    <div class="page">
      <h1 class="page-title">Az oldal nem található</h1>
      <p class="page-lead">A keresett cím nem létezik, vagy elköltözött.</p>
      <a matButton="filled" routerLink="/">Vissza a kezdőlapra</a>
    </div>
  `,
})
export class NotFoundPage {}
