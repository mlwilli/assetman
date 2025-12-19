import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, map, of, startWith, switchMap, tap } from 'rxjs';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { LocationApi } from '../../core/locations/location.api';
import {
  CreateLocationRequest,
  LocationDto,
  LocationType,
  UpdateLocationRequest,
} from '../../core/locations/location.models';
import { AuthService } from '../../core/auth/auth.service';
import { LocationPickerComponent } from '../../shared/location-picker/location-picker';

const LOCATION_TYPES: LocationType[] = [
  'COUNTRY',
  'REGION',
  'SITE',
  'BUILDING',
  'FLOOR',
  'ROOM',
  'OTHER',
];

type ViewState =
  | { kind: 'loading'; mode: 'create' | 'edit'; id?: string }
  | { kind: 'error'; mode: 'create' | 'edit'; message: string }
  | { kind: 'ready'; mode: 'create' | 'edit'; location?: LocationDto };

@Component({
  selector: 'app-location-form-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressBarModule,
    LocationPickerComponent,
  ],
  templateUrl: './location-form.html',
  styleUrl: './location-form.scss',
})
export class LocationFormPageComponent {
  private readonly api = inject(LocationApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  readonly types = LOCATION_TYPES;

  get canManageLocations(): boolean {
    const roles = this.normalizeRoles((this.auth.currentUser as any)?.roles);
    return this.hasAnyRole(roles, ['OWNER', 'ADMIN', 'MANAGER']);
  }

  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    type: ['' as '' | LocationType, [Validators.required]],
    code: ['', [Validators.maxLength(64)]],
    parentId: [''],
    active: [true],
    sortOrder: [null as number | null],
    description: ['', [Validators.maxLength(1024)]],
    externalRef: ['', [Validators.maxLength(128)]],
    customFieldsJson: [''],
  });

  get selectedParentId(): string | null {
    const v = (this.form.get('parentId')?.value ?? '').toString().trim();
    return v ? v : null;
  }

  onParentSelected(id: string | null) {
    this.form.patchValue({ parentId: id ?? '' });
  }

  readonly state$ = this.route.paramMap.pipe(
    map(pm => pm.get('id')),
    switchMap(id => {
      const mode: 'create' | 'edit' = id ? 'edit' : 'create';

      if (!this.canManageLocations) {
        return of({
          kind: 'error',
          mode,
          message: 'You do not have permission to manage locations.',
        } as ViewState);
      }

      if (!id) {
        this.form.reset({
          name: '',
          type: '' as any,
          code: '',
          parentId: '',
          active: true,
          sortOrder: null,
          description: '',
          externalRef: '',
          customFieldsJson: '',
        });
        return of({ kind: 'ready', mode } as ViewState);
      }

      return this.api.get(id).pipe(
        tap(loc => this.patchForm(loc)),
        map(loc => ({ kind: 'ready', mode, location: loc } as ViewState)),
        startWith({ kind: 'loading', mode, id } as ViewState),
        catchError(err => of(this.toErrorState(err, mode))),
      );
    }),
  );

  submit(mode: 'create' | 'edit', existing?: LocationDto) {
    if (!this.canManageLocations) return;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const req = this.toRequest(mode);

    const obs =
      mode === 'create'
        ? this.api.create(req as CreateLocationRequest)
        : this.api.update(existing!.id, req as UpdateLocationRequest);

    obs.subscribe({
      next: async saved => {
        await this.router.navigateByUrl(`/locations/${saved.id}`);
      },
      error: () => {
        // errorInterceptor likely surfaces; keep form stable
      },
    });
  }

  private patchForm(l: LocationDto) {
    this.form.patchValue({
      name: l.name ?? '',
      type: (l.type ?? '') as any,
      code: l.code ?? '',
      parentId: l.parentId ?? '',
      active: l.active ?? true,
      sortOrder: l.sortOrder ?? null,
      description: l.description ?? '',
      externalRef: l.externalRef ?? '',
      customFieldsJson: l.customFieldsJson ?? '',
    });
  }

  private toRequest(_mode: 'create' | 'edit'): CreateLocationRequest | UpdateLocationRequest {
    const v = this.form.value;

    const clean = (x: unknown): string | null => {
      const s = String(x ?? '').trim();
      return s ? s : null;
    };

    return {
      name: String(v.name ?? '').trim(),
      type: String(v.type ?? '').trim() as LocationType,
      code: clean(v.code),
      parentId: clean(v.parentId),
      active: Boolean(v.active),
      sortOrder: v.sortOrder ?? null,
      description: clean(v.description),
      externalRef: clean(v.externalRef),
      customFieldsJson: clean(v.customFieldsJson),
    };
  }

  private toErrorState(err: unknown, mode: 'create' | 'edit'): ViewState {
    if (err instanceof HttpErrorResponse) {
      const msg =
        (err.error as any)?.message ||
        err.message ||
        `Request failed (${err.status})`;
      return { kind: 'error', mode, message: msg };
    }
    return { kind: 'error', mode, message: 'Unexpected error while loading location.' };
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
