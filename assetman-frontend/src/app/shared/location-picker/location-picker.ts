import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { LocationApi } from '../../core/locations/location.api';
import { LocationTreeNodeDto } from '../../core/locations/location.models';
import {LocationChildrenComponent} from './location-children';

@Component({
  selector: 'app-location-picker',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatProgressBarModule, LocationChildrenComponent],
  templateUrl: './location-picker.html',
  styleUrls: ['./location-picker.scss'],
})
export class LocationPickerComponent {
  private readonly api = inject(LocationApi);

  @Input() selectedId: string | null = null;
  @Output() selectedIdChange = new EventEmitter<string | null>();

  readonly tree$ = this.api.getTree();

  select(id: string | null) {
    this.selectedId = id;
    this.selectedIdChange.emit(id);
  }

  trackById(_: number, n: LocationTreeNodeDto) {
    return n.id;
  }
}
