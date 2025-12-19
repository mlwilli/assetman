import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import {
  BehaviorSubject,
  catchError,
  combineLatest,
  debounceTime,
  distinctUntilChanged,
  map,
  of,
  startWith,
  switchMap,
} from 'rxjs';

import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDividerModule } from '@angular/material/divider';

import { LocationApi } from '../../core/locations/location.api';
import {
  LocationDto,
  LocationTreeNodeDto,
  LocationType,
} from '../../core/locations/location.models';
import { AuthService } from '../../core/auth/auth.service';
import { LocationTreeComponent } from '../../shared/location-tree/location-tree';

type ViewMode = 'tree' | 'list';

type ViewState =
  | { kind: 'loading'; view: ViewMode }
  | { kind: 'error'; view: ViewMode; message: string }
  | { kind: 'ready'; view: ViewMode; tree: LocationTreeNodeDto[]; list: LocationDto[] };

const LOCATION_TYPES: LocationType[] = [
  'COUNTRY',
  'REGION',
  'SITE',
  'BUILDING',
  'FLOOR',
  'ROOM',
  'OTHER',
];

@Component({
  selector: 'app-locations-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,

    // material
    MatCardModule,
    MatButtonModule,
    MatProgressBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonToggleModule,
    MatSlideToggleModule,
    MatDividerModule,

    // shared tree renderer
    LocationTreeComponent,
  ],
  templateUrl: './locations.html',
  styleUrl: './locations.scss',
})
export class LocationsPageComponent {
  private readonly api = inject(LocationApi);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly types = LOCATION_TYPES;

  private readonly viewModeSubject = new BehaviorSubject<ViewMode>('tree');
  readonly viewMode$ = this.viewModeSubject.asObservable();

  readonly filters = this.fb.nonNullable.group({
    search: this.fb.nonNullable.control(''),
    type: this.fb.nonNullable.control<'' | LocationType>(''),
    activeOnly: this.fb.nonNullable.control(true),
    parentId: this.fb.nonNullable.control(''),
  });

  get canManageLocations(): boolean {
    const roles = this.normalizeRoles((this.auth.currentUser as any)?.roles);
    return this.hasAnyRole(roles, ['OWNER', 'ADMIN', 'MANAGER']);
  }

  setViewMode(mode: ViewMode) {
    this.viewModeSubject.next(mode);
  }

  clearFilters() {
    this.filters.reset({
      search: '',
      type: '' as any,
      activeOnly: true,
      parentId: '',
    });
  }

  readonly state$ = combineLatest([
    this.viewMode$,
    this.filters.valueChanges.pipe(
      startWith(this.filters.getRawValue()),
      debounceTime(200),
      map(v => ({
        search: String(v.search ?? '').trim(),
        type: (v.type ?? '') as '' | LocationType,
        activeOnly: Boolean(v.activeOnly),
        parentId: String(v.parentId ?? '').trim(),
      })),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
    ),
  ]).pipe(
    switchMap(([view, f]) => {
      if (view === 'tree') {
        // On load (default view), only call /tree
        return this.api.getTree().pipe(
          map((tree): ViewState => ({ kind: 'ready', view, tree, list: [] })),
          startWith({ kind: 'loading', view } as ViewState),
          catchError((err: unknown) => of(this.toErrorState(err, view))),
        );
      }

      // list view: call /locations with filters
      const listParams = {
        search: f.search || undefined,
        type: f.type ? (f.type as LocationType) : undefined,
        active: f.activeOnly ? true : undefined,
        parentId: f.parentId || undefined,
      };

      return this.api.list(listParams).pipe(
        map((list): ViewState => ({ kind: 'ready', view, tree: [], list })),
        startWith({ kind: 'loading', view } as ViewState),
        catchError((err: unknown) => of(this.toErrorState(err, view))),
      );
    }),
  );

  trackById(_: number, n: { id: string }) {
    return n.id;
  }

  private toErrorState(err: unknown, view: ViewMode): ViewState {
    if (err instanceof HttpErrorResponse) {
      const msg =
        (err.error as any)?.message ||
        err.message ||
        `Request failed (${err.status})`;
      return { kind: 'error', view, message: msg };
    }
    return { kind: 'error', view, message: 'Unexpected error while loading locations.' };
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
