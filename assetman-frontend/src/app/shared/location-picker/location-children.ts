import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { LocationTreeNodeDto } from '../../core/locations/location.models';

@Component({
  selector: 'app-location-children',
  standalone: true,
  imports: [CommonModule],
  template: `
    <ul class="children" *ngIf="nodes.length">
      <li *ngFor="let n of nodes">
        <button
          type="button"
          class="node"
          [class.selected]="n.id === selectedId"
          (click)="select.emit(n.id)"
        >
          {{ n.name }}
        </button>

        <app-location-children
          [nodes]="n.children"
          [selectedId]="selectedId"
          (select)="select.emit($event)"
        ></app-location-children>
      </li>
    </ul>
  `,
  styles: [`
    .children { list-style: none; padding-left: 12px; margin: 0; }
    .node { background: none; border: 0; cursor: pointer; padding: 4px 6px; text-align: left; }
    .selected { font-weight: 600; text-decoration: underline; }
  `]
})
export class LocationChildrenComponent {
  @Input() nodes: LocationTreeNodeDto[] = [];
  @Input() selectedId: string | null = null;
  @Output() select = new EventEmitter<string>();
}
