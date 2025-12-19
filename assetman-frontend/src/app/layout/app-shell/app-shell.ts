import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { take } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';

type NavItem = {
  label: string;
  icon: string;
  link: string;
  exact?: boolean;
};

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,

    // router
    RouterLink,
    RouterLinkActive,

    // material
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
  ],
  templateUrl: './app-shell.html',
  styleUrls: ['./app-shell.scss'],
})
export class AppShellComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly user$ = this.auth.user$;

  readonly navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', link: '/dashboard', exact: true },
    { label: 'Assets', icon: 'inventory_2', link: '/assets' },
    { label: 'Locations', icon: 'account_tree', link: '/locations' },
  ];

  readonly userLabel = computed(() => {
    const u = this.auth.currentUser;
    if (!u) return '';
    const name = (u.fullName ?? '').trim();
    return name.length ? name : u.email;
  });

  logout(): void {
    this.auth
      .logout()
      .pipe(take(1))
      .subscribe({
        next: () => {
          void this.router.navigateByUrl('/login');
        },
        error: () => {
          // logout() already clears locally even if backend fails; still route to login
          void this.router.navigateByUrl('/login');
        },
      });
  }
}
