import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges } from '@angular/core';
import { RouterLink } from '@angular/router';

import { MatTreeModule } from '@angular/material/tree';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

import { NestedTreeControl } from '@angular/cdk/tree';
import { MatTreeNestedDataSource } from '@angular/material/tree';

import { LocationTreeNodeDto } from '../../core/locations/location.models';

export type LocationTreeMode = 'navigate' | 'select';

@Component({
  selector: 'app-location-tree',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatTreeModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './location-tree.html',
  styleUrls: ['./location-tree.scss'],
})
export class LocationTreeComponent implements OnChanges {
  @Input() nodes: LocationTreeNodeDto[] = [];

  /** navigate => click goes to /locations/:id ; select => click emits selected id */
  @Input() mode: LocationTreeMode = 'navigate';

  @Input() selectedId: string | null = null;
  @Output() selectedIdChange = new EventEmitter<string | null>();

  readonly treeControl = new NestedTreeControl<LocationTreeNodeDto>(n => n.children ?? []);
  readonly dataSource = new MatTreeNestedDataSource<LocationTreeNodeDto>();

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['nodes']) return;

    // Preserve expansion state across refreshes
    const expandedIds = new Set(
      (this.treeControl.expansionModel.selected ?? []).map(n => n.id),
    );

    this.dataSource.data = this.nodes ?? [];

    // Re-expand any nodes that still exist
    const index = new Map<string, LocationTreeNodeDto>();
    const walk = (arr: LocationTreeNodeDto[]) => {
      for (const n of arr ?? []) {
        index.set(n.id, n);
        if (n.children?.length) walk(n.children);
      }
    };
    walk(this.dataSource.data);

    this.treeControl.collapseAll();
    for (const id of expandedIds) {
      const node = index.get(id);
      if (node) this.treeControl.expand(node);
    }
  }

  hasChild = (_: number, node: LocationTreeNodeDto) =>
    Array.isArray(node.children) && node.children.length > 0;

  toggle(node: LocationTreeNodeDto): void {
    this.treeControl.toggle(node);
  }

  onNodeClick(node: LocationTreeNodeDto): void {
    if (this.mode !== 'select') return;
    this.selectedId = node.id;
    this.selectedIdChange.emit(node.id);
  }

  clearSelection(): void {
    if (this.mode !== 'select') return;
    this.selectedId = null;
    this.selectedIdChange.emit(null);
  }

  trackById = (_: number, n: LocationTreeNodeDto) => n.id;
}
