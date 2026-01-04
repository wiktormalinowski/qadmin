import { Component, Input } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { EntityViewModel } from '../entity/entity.model';
import { DynamicTableComponent } from '../dynamic-table/dynamic-table.component';

@Component({
  selector: 'app-entity-card',
  standalone: true,
  imports: [JsonPipe, DynamicTableComponent],
  templateUrl: 'entity-card.component.html',
  styleUrls: ['entity-card.component.css']
})
export class EntityCardComponent {
  /** Obiekt encji zawierający dane i metadane. */
  @Input({ required: true }) entity!: EntityViewModel;

  get recordCount(): number {
    return Array.isArray(this.entity.data) ? this.entity.data.length : 0;
  }
}
