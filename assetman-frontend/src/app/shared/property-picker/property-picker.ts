import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  Output,
  SimpleChanges,
  inject,
} from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  of,
  startWith,
  switchMap,
  tap,
  shareReplay,
} from 'rxjs';

import { PropertyApi } from '../../core/properties/property.api';
import { PropertyDto, propertyLabel } from '../../core/properties/property.models';

@Component({
  selector: 'app-property-picker',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatAutocompleteModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  templateUrl: './property-picker.html',
  styleUrls: ['./property-picker.scss'],
})
export class PropertyPickerComponent {
  private readonly api = inject(PropertyApi);

  @Input() selectedId: string | null = null;
  @Output() selectedIdChange = new EventEmitter<string | null>();

  readonly ctrl = new FormControl<string>('', { nonNullable: true });

  loading = false;

  /**
   * Stream of search results. Template MUST subscribe (async pipe),
   * otherwise the HTTP calls will never run.
   */
  readonly options$ = this.ctrl.valueChanges.pipe(
    startWith(this.ctrl.value),
    debounceTime(200),
    distinctUntilChanged(),
    switchMap((term) => {
      this.loading = true;
      const q = (term ?? '').trim() || undefined;

      return this.api.list({ search: q }).pipe(
        catchError(() => of([] as PropertyDto[])),
        tap(() => (this.loading = false)),
      );
    }),
    // keep last list to avoid flicker when opening/closing autocomplete
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  ngOnChanges(changes: SimpleChanges): void {
    // Edit mode: if parent provides an id, resolve it once to a label.
    if (!changes['selectedId']) return;

    const id = (this.selectedId ?? '').trim();
    if (!id) return;

    this.api.get(id).subscribe({
      next: (p) => this.ctrl.setValue(propertyLabel(p), { emitEvent: false }),
      error: () => {
        // Keep stable even if lookup fails.
      },
    });
  }

  displayFn = (x: unknown) => String(x ?? '');

  pick(p: PropertyDto | null) {
    if (!p) {
      this.clear();
      return;
    }
    this.ctrl.setValue(propertyLabel(p), { emitEvent: false });
    this.selectedId = p.id;
    this.selectedIdChange.emit(p.id);
  }

  clear() {
    this.ctrl.setValue('', { emitEvent: false });
    this.selectedId = null;
    this.selectedIdChange.emit(null);
  }
}
