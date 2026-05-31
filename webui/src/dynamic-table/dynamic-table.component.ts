import { Component, Input, inject, ChangeDetectorRef } from '@angular/core';
import { KeyValuePipe } from '@angular/common';
import { EntityService } from '../entity/entity.service';

@Component({
  selector: 'app-dynamic-table',
  standalone: true,
  imports: [KeyValuePipe],
  templateUrl: 'dynamic-table.component.html',
  styleUrls: ['dynamic-table.component.css']
})
export class DynamicTableComponent {
  @Input({required: true}) entityName!: string;
  @Input() data: unknown;
  @Input() metadata: any;

  private entityService = inject(EntityService);
  private cdr = inject(ChangeDetectorRef);

  showModal = false;
  rowToDelete: any = null;
  isDeleting = false;
  toast: { message: string; type: 'success' | 'error' } | null = null;

  get dataAsArray(): any[] {
    return Array.isArray(this.data) ? this.data : [];
  }

  get hasData(): boolean {
    return this.dataAsArray.length > 0;
  }

  getRowId(row: any): any {
    if (!row) return null;
    return row.id || row.Id || row._id || (row.metadata && row.metadata.id) || Object.values(row)[0];
  }

  getDisplayValue(key: any, value: any): any {
    if (value && typeof value === 'object') {
       if (this.metadata && typeof this.metadata === 'object' && !Array.isArray(this.metadata)) {
          const keyStr = String(key);
          const meta = this.metadata[keyStr];
          if (meta && meta.displayAttribute) {
              const displayAttr = meta.displayAttribute;
              return value[displayAttr] !== undefined ? value[displayAttr] : (value.id || '[object Object]');
          }
       }
       return value.id || '[object Object]';
    }
    return value;
  }

  confirmDelete(row: any) {
    this.rowToDelete = row;
    this.showModal = true;
  }

  cancelDelete() {
    this.showModal = false;
    this.rowToDelete = null;
  }

  showToast(message: string, type: 'success' | 'error') {
    this.toast = { message, type };
    this.cdr.markForCheck();
    setTimeout(() => {
      this.toast = null;
      this.cdr.markForCheck();
    }, 4000);
  }

  private getErrorMessage(err: any, fallback: string): string {
    if (!err) return fallback;
    if (typeof err.error === 'string') return err.error;
    if (err.error && typeof err.error.text === 'string') return err.error.text; // Handles JSON parsing errors where the backend returns a plain string
    if (err.error && typeof err.error.message === 'string') return err.error.message;
    if (typeof err.message === 'string') return err.message;
    return fallback;
  }

  executeDelete() {
    if (!this.rowToDelete) return;
    const id = this.getRowId(this.rowToDelete);
    this.isDeleting = true;
    this.entityService.deleteEntity(this.entityName, id).subscribe({
      next: () => {
        if (Array.isArray(this.data)) {
          this.data = this.data.filter(r => r !== this.rowToDelete);
        }
        this.isDeleting = false;
        this.showModal = false;
        this.showToast(`Successfully deleted ${this.entityName} with ID: ${id}`, 'success');
        this.rowToDelete = null;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to delete', err);
        this.isDeleting = false;
        this.showModal = false;
        const errMsg = this.getErrorMessage(err, `Failed to delete ${this.entityName} with ID: ${id}`);
        this.showToast(errMsg, 'error');
        this.cdr.markForCheck();
      }
    });
  }
}
