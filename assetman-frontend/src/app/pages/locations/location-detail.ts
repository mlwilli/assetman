import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, map, of, startWith, switchMap } from 'rxjs';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { LocationApi } from '../../core/locations/location.api';
import { LocationDto } from '../../core/locations/location.models';
import { AuthService } from '../../core/auth/auth.service';

type ViewState =
  | { kind: 'loading'; id: string }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; location: LocationDto };

@Component({
  selector: 'app-location-detail-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatProgressBarModule,
  ],
  templateUrl: './location-detail.html',
  styleUrl: './location-detail.scss',
})
export class LocationDetailPageComponent {
  private readonly api = inject(LocationApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  get canManageLocations(): boolean {
    const roles = this.normalizeRoles((this.auth.currentUser as any)?.roles);
    return this.hasAnyRole(roles, ['OWNER', 'ADMIN', 'MANAGER']);
  }

  readonly state$ = this.route.paramMap.pipe(
    map(pm => pm.get('id')),
    switchMap(id => {
      if (!id) return of({ kind: 'error', message: 'Missing location id.' } as ViewState);

      return this.api.get(id).pipe(
        map((location): ViewState => ({ kind: 'ready', location })),
        startWith({ kind: 'loading', id } as ViewState),
        catchError((err: unknown) => of(this.toErrorState(err))),
      );
    }),
  );

  async deleteLocation(loc: LocationDto) {
    if (!this.canManageLocations) return;
    const ok = window.confirm(`Delete location "${loc.name}"?`);
    if (!ok) return;

    this.api.delete(loc.id).subscribe({
      next: async () => {
        await this.router.navigateByUrl('/locations');
      },
      error: () => {
        // errorInterceptor likely surfaces; keep page stable
      },
    });
  }

  private toErrorState(err: unknown): ViewState {
    if (err instanceof HttpErrorResponse) {
      const msg =
        (err.error as any)?.message ||
        err.message ||
        `Request failed (${err.status})`;
      return { kind: 'error', message: msg };
    }
    return { kind: 'error', message: 'Unexpected error while loading location.' };
  }

  private normalizeRoles(raw: unknown): string[] {
    if (!raw) return [];
    if (Array.isArray(raw)) return raw.map(String);
    if (raw instanceof Set) return Array.from(raw).map(String);
    return [];
  }

  private hasAnyRole(userRoles: string[], required: string[]): boolean {
    const set = new Set(userRoles);
    return required.some(r => set.has(r));
  }
}
