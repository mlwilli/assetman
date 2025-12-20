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

import { UserApi } from '../../core/users/user.api';
import { UserDirectoryDto, userLabel } from '../../core/users/user.models';

@Component({
  selector: 'app-user-picker',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatAutocompleteModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  templateUrl: './user-picker.html',
  styleUrls: ['./user-picker.scss'],
})
export class UserPickerComponent {
  private readonly api = inject(UserApi);

  @Input() selectedId: string | null = null;
  @Output() selectedIdChange = new EventEmitter<string | null>();

  readonly ctrl = new FormControl<string>('', { nonNullable: true });

  loading = false;

  readonly options$ = this.ctrl.valueChanges.pipe(
    startWith(this.ctrl.value),
    debounceTime(200),
    distinctUntilChanged(),
    switchMap((term) => {
      this.loading = true;
      const q = (term ?? '').trim() || undefined;

      return this.api.list({ search: q, limit: 20, activeOnly: true }).pipe(
        catchError(() => of([] as UserDirectoryDto[])),
        tap(() => (this.loading = false)),
      );
    }),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['selectedId']) return;

    const id = (this.selectedId ?? '').trim();
    if (!id) return;

    // Requires backend GET /api/users/{id}
    this.api.get(id).subscribe({
      next: (u) => this.ctrl.setValue(userLabel(u), { emitEvent: false }),
      error: () => {
        // keep stable
      },
    });
  }

  displayFn = (x: unknown) => String(x ?? '');

  pick(u: UserDirectoryDto | null) {
    if (!u) {
      this.clear();
      return;
    }
    this.ctrl.setValue(userLabel(u), { emitEvent: false });
    this.selectedId = u.id;
    this.selectedIdChange.emit(u.id);
  }

  clear() {
    this.ctrl.setValue('', { emitEvent: false });
    this.selectedId = null;
    this.selectedIdChange.emit(null);
  }

  protected readonly userLabel = userLabel;
}
