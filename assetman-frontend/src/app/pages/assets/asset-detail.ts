import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, map, of, startWith, switchMap } from 'rxjs';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { AssetApi } from '../../core/assets/asset.api';
import { AssetDto } from '../../core/assets/asset.models';
import { AuthService } from '../../core/auth/auth.service';

type ViewState =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; asset: AssetDto };

@Component({
  selector: 'app-asset-detail-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatProgressBarModule,
  ],
  templateUrl: './asset-detail.html',
  styleUrl: './asset-detail.scss',
})
export class AssetDetailPageComponent {
  private readonly api = inject(AssetApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  get canManageAssets(): boolean {
    const roles = this.normalizeRoles((this.auth.currentUser as any)?.roles);
    return this.hasAnyRole(roles, ['OWNER', 'ADMIN', 'MANAGER']);
  }

  readonly state$ = this.route.paramMap.pipe(
    map(pm => pm.get('id') ?? ''),
    switchMap(id => {
      if (!id) return of({ kind: 'error', message: 'Missing asset id.' } as ViewState);

      return this.api.getAsset(id).pipe(
        map(asset => ({ kind: 'ready', asset } as ViewState)),
        startWith({ kind: 'loading' } as ViewState),
        catchError(err => of(this.toErrorState(err))),
      );
    }),
  );

  async delete(asset: AssetDto) {
    if (!this.canManageAssets) return;

    const ok = window.confirm(`Delete asset "${asset.name}"? This cannot be undone.`);
    if (!ok) return;

    this.api.deleteAsset(asset.id).subscribe({
      next: async () => {
        await this.router.navigateByUrl('/assets');
      },
      error: () => {
        // errorInterceptor will surface; keep page stable
      },
    });
  }

  private toErrorState(err: unknown): ViewState {
    if (err instanceof HttpErrorResponse) {
      const msg = (err.error as any)?.message || err.message || `Request failed (${err.status})`;
      return { kind: 'error', message: msg };
    }
    return { kind: 'error', message: 'Unexpected error while loading asset.' };
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
