import { Component, Input } from '@angular/core';
import { KeyValuePipe } from '@angular/common';

@Component({
  selector: 'app-dynamic-table',
  standalone: true,
  imports: [KeyValuePipe],
  templateUrl: 'dynamic-table.component.html',
  styleUrls: ['dynamic-table.component.css']
})
export class DynamicTableComponent {
  /** Surowe dane do wyświetlenia w tabeli. */
  @Input() data: unknown;

  get dataAsArray(): any[] {
    return Array.isArray(this.data) ? this.data : [];
  }

  get hasData(): boolean {
    return this.dataAsArray.length > 0;
  }
}
