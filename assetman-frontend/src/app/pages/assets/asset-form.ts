import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, map, of, startWith, switchMap, tap } from 'rxjs';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';

import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';

import { PropertyPickerComponent } from '../../shared/property-picker/property-picker';
import { UnitPickerComponent } from '../../shared/unit-picker/unit-picker';
import { UserPickerComponent } from '../../shared/user-picker/user-picker';


import { AssetApi } from '../../core/assets/asset.api';
import {
  AssetDto,
  AssetStatus,
  AssetUpsertRequest,
  ASSET_STATUS_OPTIONS,
} from '../../core/assets/asset.models';
import { AuthService } from '../../core/auth/auth.service';
import { LocationPickerComponent } from '../../shared/location-picker/location-picker';

type ViewState =
  | { kind: 'loading'; mode: 'create' | 'edit'; id?: string }
  | { kind: 'error'; mode: 'create' | 'edit'; message: string }
  | { kind: 'ready'; mode: 'create' | 'edit'; asset?: AssetDto };

@Component({
  selector: 'app-asset-form-page',
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
    PropertyPickerComponent,
    UnitPickerComponent,
    UserPickerComponent,
    MatProgressBarModule,
    LocationPickerComponent,
    MatDatepickerModule,
    MatNativeDateModule,
  ],
  templateUrl: './asset-form.html',
  styleUrl: './asset-form.scss',
})
export class AssetFormPageComponent {
  private readonly api = inject(AssetApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  readonly statusOptions = ASSET_STATUS_OPTIONS;

  get canManageAssets(): boolean {
    const roles = this.normalizeRoles((this.auth.currentUser as any)?.roles);
    return this.hasAnyRole(roles, ['OWNER', 'ADMIN', 'MANAGER']);
  }

  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    status: ['' as '' | AssetStatus, [Validators.required]],

    category: [''],
    code: [''],
    assetTag: [''],
    serialNumber: [''],
    manufacturer: [''],
    model: [''],
    externalRef: [''],

    tagsCsv: [''],

    locationId: [''],
    propertyId: [''],
    unitId: [''],
    assignedUserId: [''],

    purchaseDate: [null as Date | null],
    inServiceDate: [null as Date | null],
    retiredDate: [null as Date | null],
    disposedDate: [null as Date | null],
    warrantyExpiryDate: [null as Date | null],

    purchaseCost: [''],
    residualValue: [''],
    depreciationYears: [''],

    customFieldsJson: [''],
  });

  get selectedLocationId(): string | null {
    const v = (this.form.get('locationId')?.value ?? '').toString().trim();
    return v ? v : null;
  }

  onLocationSelected(id: string | null) {
    this.form.patchValue({ locationId: id ?? '' });
  }

  readonly state$ = this.route.paramMap.pipe(
    map((pm) => pm.get('id')),
    switchMap((id) => {
      const mode: 'create' | 'edit' = id ? 'edit' : 'create';

      if (!this.canManageAssets) {
        return of({
          kind: 'error',
          mode,
          message: 'You do not have permission to manage assets.',
        } as ViewState);
      }

      if (!id) {
        this.form.reset({
          name: '',
          status: '' as any,
          tagsCsv: '',
          locationId: '',

          purchaseDate: null,
          inServiceDate: null,
          retiredDate: null,
          disposedDate: null,
          warrantyExpiryDate: null,
        });
        return of({ kind: 'ready', mode } as ViewState);
      }


      return this.api.getAsset(id).pipe(
        tap((asset) => this.patchForm(asset)),
        map((asset) => ({ kind: 'ready', mode, asset } as ViewState)),
        startWith({ kind: 'loading', mode, id } as ViewState),
        catchError((err) => of(this.toErrorState(err, mode))),
      );
    }),
  );

  submit(mode: 'create' | 'edit', existing?: AssetDto) {
    if (!this.canManageAssets) return;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const req = this.toUpsertRequest();

    const obs =
      mode === 'create'
        ? this.api.createAsset(req)
        : this.api.updateAsset(existing!.id, req);

    obs.subscribe({
      next: async (saved) => {
        await this.router.navigateByUrl(`/assets/${saved.id}`);
      },
      error: () => {
        // errorInterceptor likely surfaces; keep form stable
      },
    });
  }

  private patchForm(a: AssetDto) {
    this.form.patchValue({
      name: a.name ?? '',
      status: (a.status ?? '') as any,
      category: a.category ?? '',
      code: a.code ?? '',
      assetTag: a.assetTag ?? '',
      serialNumber: a.serialNumber ?? '',
      manufacturer: a.manufacturer ?? '',
      model: a.model ?? '',
      externalRef: a.externalRef ?? '',
      tagsCsv: a.tags?.length ? a.tags.join(', ') : '',

      locationId: a.locationId ?? '',
      propertyId: a.propertyId ?? '',
      unitId: a.unitId ?? '',
      assignedUserId: a.assignedUserId ?? '',

      purchaseDate: this.parseIsoDate(a.purchaseDate),
      inServiceDate: this.parseIsoDate(a.inServiceDate),
      retiredDate: this.parseIsoDate(a.retiredDate),
      disposedDate: this.parseIsoDate(a.disposedDate),
      warrantyExpiryDate: this.parseIsoDate(a.warrantyExpiryDate),


      purchaseCost: a.purchaseCost != null ? String(a.purchaseCost) : '',
      residualValue: a.residualValue != null ? String(a.residualValue) : '',
      depreciationYears: a.depreciationYears != null ? String(a.depreciationYears) : '',

      customFieldsJson: a.customFieldsJson ?? '',
    });
  }

  private toUpsertRequest(): AssetUpsertRequest {
    const v = this.form.value;

    const parseNumberOrNull = (x: unknown): number | null => {
      if (x === null || x === undefined) return null;
      const s = String(x).trim();
      if (!s) return null;
      const n = Number(s);
      return Number.isFinite(n) ? n : null;
    };

    const parseIntOrNull = (x: unknown): number | null => {
      const n = parseNumberOrNull(x);
      if (n === null) return null;
      return Number.isFinite(n) ? Math.trunc(n) : null;
    };

    const clean = (x: unknown): string | null => {
      const s = String(x ?? '').trim();
      return s ? s : null;
    };

    const tags = (v.tagsCsv ?? '')
      .split(',')
      .map((t) => t.trim())
      .filter(Boolean);

    return {
      name: String(v.name ?? '').trim(),
      status: String(v.status ?? '').trim() as AssetStatus,

      category: clean(v.category),
      code: clean(v.code),
      assetTag: clean(v.assetTag),
      serialNumber: clean(v.serialNumber),
      manufacturer: clean(v.manufacturer),
      model: clean(v.model),
      externalRef: clean(v.externalRef),

      tags: tags.length ? tags : null,

      locationId: clean(v.locationId),
      propertyId: clean(v.propertyId),
      unitId: clean(v.unitId),
      assignedUserId: clean(v.assignedUserId),

      purchaseDate: this.toIsoDate(v.purchaseDate),
      inServiceDate: this.toIsoDate(v.inServiceDate),
      retiredDate: this.toIsoDate(v.retiredDate),
      disposedDate: this.toIsoDate(v.disposedDate),
      warrantyExpiryDate: this.toIsoDate(v.warrantyExpiryDate),

      purchaseCost: parseNumberOrNull(v.purchaseCost),
      residualValue: parseNumberOrNull(v.residualValue),
      depreciationYears: parseIntOrNull(v.depreciationYears),

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
    return {
      kind: 'error',
      mode,
      message: 'Unexpected error while loading asset.',
    };
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

  private parseIsoDate(value: string | null | undefined): Date | null {
    if (!value) return null;
    // Expecting YYYY-MM-DD from backend
    const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
    if (!m) return null;

    const year = Number(m[1]);
    const month = Number(m[2]); // 1-12
    const day = Number(m[3]);

    // Construct as local date (no timezone shifting surprises from Date.parse)
    const d = new Date(year, month - 1, day);
    return Number.isNaN(d.getTime()) ? null : d;
  }

  private toIsoDate(value: unknown): string | null {
    if (!value) return null;

    // If datepicker gave us a Date
    if (value instanceof Date) {
      if (Number.isNaN(value.getTime())) return null;
      const y = value.getFullYear();
      const m = String(value.getMonth() + 1).padStart(2, '0');
      const d = String(value.getDate()).padStart(2, '0');
      return `${y}-${m}-${d}`;
    }

    // If something else slipped in (string), only accept strict YYYY-MM-DD
    const s = String(value).trim();
    if (!s) return null;
    return /^\d{4}-\d{2}-\d{2}$/.test(s) ? s : null;
  }

  get selectedUnitId(): string | null {
    const v = (this.form.get('unitId')?.value ?? '').toString().trim();
    return v ? v : null;
  }

  get selectedAssignedUserId(): string | null {
    const v = (this.form.get('assignedUserId')?.value ?? '').toString().trim();
    return v ? v : null;
  }

  onUnitSelected(id: string | null) {
    this.form.patchValue({ unitId: id ?? '' });
  }

  onAssignedUserSelected(id: string | null) {
    this.form.patchValue({ assignedUserId: id ?? '' });
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



}
