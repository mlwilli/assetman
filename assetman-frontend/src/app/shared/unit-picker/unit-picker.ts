import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import {
  debounceTime,
  distinctUntilChanged,
  of,
  startWith,
  switchMap,
  tap,
  catchError,
} from 'rxjs';

import { UnitApi } from '../../core/units/unit.api';
import { UnitDto, unitLabel } from '../../core/units/unit.models';

@Component({
  selector: 'app-unit-picker',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatAutocompleteModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  templateUrl: './unit-picker.html',
  styleUrls: ['./unit-picker.scss'],
})
export class UnitPickerComponent {
  private readonly api = inject(UnitApi);

  @Input() propertyId: string | null = null;

  @Input() selectedId: string | null = null;
  @Output() selectedIdChange = new EventEmitter<string | null>();

  // Start disabled until a property is selected
  readonly ctrl = new FormControl<string>(
    { value: '', disabled: true },
    { nonNullable: true },
  );

  loading = false;
  options: UnitDto[] = [];

  readonly options$ = this.ctrl.valueChanges.pipe(
    startWith(this.ctrl.value),
    debounceTime(200),
    distinctUntilChanged(),
    tap(() => (this.loading = true)),
    switchMap((term) => {
      const pid = (this.propertyId ?? '').trim();
      if (!pid) {
        this.options = [];
        this.loading = false;
        return of([]);
      }

      return this.api
        .list({ propertyId: pid, search: (term ?? '').trim() || undefined })
        .pipe(
          tap((items) => {
            this.options = items;
            this.loading = false;
          }),
          catchError(() => {
            this.options = [];
            this.loading = false;
            return of([]);
          }),
        );
    }),
  );

  ngOnChanges(): void {
    // Enable/disable based on propertyId (reactive-forms-friendly)
    const pid = (this.propertyId ?? '').trim();
    if (!pid) {
      this.ctrl.disable({ emitEvent: false });
      this.ctrl.setValue('', { emitEvent: false });
      this.options = [];
      return;
    } else {
      this.ctrl.enable({ emitEvent: false });
    }

    // Resolve label for edit mode
    const id = (this.selectedId ?? '').trim();
    if (!id) return;

    this.api.get(id).subscribe({
      next: (u) => this.ctrl.setValue(unitLabel(u), { emitEvent: false }),
      error: () => {},
    });
  }

  displayFn = (x: unknown) => String(x ?? '');

  pick(u: UnitDto) {
    this.ctrl.setValue(unitLabel(u), { emitEvent: false });
    this.selectedId = u.id;
    this.selectedIdChange.emit(u.id);
  }

  clear() {
    this.ctrl.setValue('', { emitEvent: false });
    this.selectedId = null;
    this.selectedIdChange.emit(null);
  }
}
