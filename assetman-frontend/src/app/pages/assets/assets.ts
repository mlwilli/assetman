import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import {
  BehaviorSubject,
  combineLatest,
  debounceTime,
  distinctUntilChanged,
  map,
  startWith,
  switchMap,
  catchError,
  of,
  tap,
} from 'rxjs';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';

import { RouterLink } from '@angular/router';

import { AssetApi } from '../../core/assets/asset.api';
import {
  AssetDto,
  AssetStatus,
  ASSET_STATUS_OPTIONS,
  PageResponse,
} from '../../core/assets/asset.models';
import { AuthService } from '../../core/auth/auth.service';

import { LocationPickerComponent } from '../../shared/location-picker/location-picker';
import { PropertyPickerComponent } from '../../shared/property-picker/property-picker';
import { UnitPickerComponent } from '../../shared/unit-picker/unit-picker';
import { UserPickerComponent } from '../../shared/user-picker/user-picker';

type ViewState =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'ready'; page: PageResponse<AssetDto> };

@Component({
  selector: 'app-assets-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressBarModule,
    LocationPickerComponent,
    PropertyPickerComponent,
    UnitPickerComponent,
    UserPickerComponent,
  ],
  templateUrl: './assets.html',
  styleUrl: './assets.scss',
})
export class AssetsPageComponent {
  private readonly api = inject(AssetApi);
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  readonly statusOptions = ASSET_STATUS_OPTIONS;

  readonly form = this.fb.group({
    search: [''],
    status: ['' as '' | AssetStatus],
    category: [''],

    locationId: [''],
    propertyId: [''],
    unitId: [''],
    assignedUserId: [''],
  });

  private readonly pageSubject = new BehaviorSubject<number>(0);
  private readonly sizeSubject = new BehaviorSubject<number>(20);

  readonly page$ = this.pageSubject.asObservable();
  readonly size$ = this.sizeSubject.asObservable();

  get canManageAssets(): boolean {
    const roles = this.normalizeRoles((this.auth.currentUser as any)?.roles);
    return this.hasAnyRole(roles, ['OWNER', 'ADMIN', 'MANAGER']);
  }

  readonly state$ = combineLatest([
    this.form.valueChanges.pipe(
      startWith(this.form.value),
      debounceTime(200),
      map((v) => ({
        search: (v.search ?? '').trim(),
        status: (v.status ?? '').trim(),
        category: (v.category ?? '').trim(),
        locationId: (v.locationId ?? '').trim(),
        propertyId: (v.propertyId ?? '').trim(),
        unitId: (v.unitId ?? '').trim(),
        assignedUserId: (v.assignedUserId ?? '').trim(),
      })),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      tap(() => this.pageSubject.next(0)),
    ),
    this.page$,
    this.size$,
  ]).pipe(
    switchMap(([filters, page, size]) => {
      const req = {
        page,
        size,
        filters: {
          search: filters.search || null,
          status: (filters.status || null) as AssetStatus | null,
          category: filters.category || null,
          locationId: filters.locationId || null,
          propertyId: filters.propertyId || null,
          unitId: filters.unitId || null,
          assignedUserId: filters.assignedUserId || null,
        },
      };

      return this.api.listAssets(req).pipe(
        map((pageResp): ViewState => ({ kind: 'ready', page: pageResp })),
        startWith({ kind: 'loading' } as ViewState),
        catchError((err: unknown) => of(this.toErrorState(err))),
      );
    }),
  );

  nextPage(current: PageResponse<AssetDto>) {
    if (current.page + 1 >= current.totalPages) return;
    this.pageSubject.next(current.page + 1);
  }

  prevPage(current: PageResponse<AssetDto>) {
    if (current.page <= 0) return;
    this.pageSubject.next(current.page - 1);
  }

  // -------- Picker adapters (keep templates clean) --------

  get selectedLocationId(): string | null {
    const v = (this.form.get('locationId')?.value ?? '').toString().trim();
    return v ? v : null;
  }
  onLocationSelected(id: string | null) {
    this.form.patchValue({ locationId: id ?? '' });
  }

  get selectedPropertyId(): string | null {
    const v = (this.form.get('propertyId')?.value ?? '').toString().trim();
    return v ? v : null;
  }
  onPropertySelected(id: string | null) {
    const current = (this.form.get('propertyId')?.value ?? '').toString().trim();
    const next = (id ?? '').trim();

    // If property changes, clear unit to prevent inconsistent association
    if (current && next && current !== next) {
      this.form.patchValue({ unitId: '' });
    }
    if (!next) {
      this.form.patchValue({ unitId: '' });
    }

    this.form.patchValue({ propertyId: next });
  }

  get selectedUnitId(): string | null {
    const v = (this.form.get('unitId')?.value ?? '').toString().trim();
    return v ? v : null;
  }
  onUnitSelected(id: string | null) {
    this.form.patchValue({ unitId: id ?? '' });
  }

  get selectedAssignedUserId(): string | null {
    const v = (this.form.get('assignedUserId')?.value ?? '').toString().trim();
    return v ? v : null;
  }
  onAssignedUserSelected(id: string | null) {
    this.form.patchValue({ assignedUserId: id ?? '' });
  }

  // -------- Error + RBAC helpers --------

  private toErrorState(err: unknown): ViewState {
    if (err instanceof HttpErrorResponse) {
      const msg =
        (err.error as any)?.message ||
        err.message ||
        `Request failed (${err.status})`;
      return { kind: 'error', message: msg };
    }
    return { kind: 'error', message: 'Unexpected error while loading assets.' };
  }

  private normalizeRoles(raw: unknown): string[] {
    if (!raw) return [];
    if (Array.isArray(raw)) return raw.map(String);
    if (raw instanceof Set) return Array.from(raw).map(String);
    return [];
  }

  private hasAnyRole(userRoles: string[], required: string[]): boolean {
    const set = new Set(userRoles);
    return required.some((r) => set.has(r));
  }
}
