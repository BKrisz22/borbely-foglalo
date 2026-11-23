import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from '../core/auth/auth.service';
import { ROLE_LABELS, navItemsFor } from './nav-items';

/** Kis képernyőn a menü fiókként nyílik, nagy képernyőn állandóan látszik. */
const HANDSET_QUERY = '(max-width: 959.98px)';

/** Az alkalmazás kerete: felső sáv a felhasználói menüvel, oldalsó menü szerepkör szerint. */
@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatSidenavModule,
    MatListModule,
    MatMenuModule,
    MatDividerModule,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly auth = inject(AuthService);

  protected readonly user = this.auth.user;
  protected readonly navItems = computed(() => navItemsFor(this.auth.role()));
  protected readonly roleLabel = computed(() => {
    const role = this.auth.role();
    return role ? ROLE_LABELS[role] : '';
  });
  protected readonly isHandset = toSignal(
    inject(BreakpointObserver)
      .observe(HANDSET_QUERY)
      .pipe(map((state) => state.matches)),
    { initialValue: false },
  );

  protected logout(): void {
    this.auth.logout();
  }
}
