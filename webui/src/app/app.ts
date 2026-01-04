import { Component, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {AsyncPipe, JsonPipe, KeyValuePipe} from '@angular/common';
import { forkJoin, map, Observable, of, switchMap, catchError, shareReplay } from 'rxjs';

// Typed interface for better maintainability and tooling support
interface EntityViewModel {
  name: string;
  metadata: unknown;
  data: unknown;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    AsyncPipe,
    JsonPipe,
    KeyValuePipe
  ],
  templateUrl: 'app.html',
  styleUrls: ['app.css']
})
export class App {
  private http = inject(HttpClient);

  // Define the ViewModel stream.
  // We shareReplay(1) to prevent multiple HTTP calls if the template subscribes multiple times (or if used in multiple places).
  readonly vm$: Observable<EntityViewModel[]> = this.http.get<string[]>('/q/qadmin/api/entities').pipe(
    switchMap(entityNames => {
      if (!entityNames || entityNames.length === 0) {
        return of([]);
      }

      // Create an array of Observables, one for each entity
      const batchRequests = entityNames.map(name => this.fetchEntityDetails(name));

      // Execute all requests in parallel
      return forkJoin(batchRequests);
    }),
    catchError(err => {
      console.error('Critical failure loading entities', err);
      return of([]); // Return empty list on global failure
    }),
    shareReplay(1)
  );

  /**
   * Fetches both Metadata and Data for a specific entity in parallel.
   * Includes error isolation so one failing entity doesn't break the whole list.
   */
  private fetchEntityDetails(name: string): Observable<EntityViewModel> {
    const encodedName = encodeURIComponent(name);

    return forkJoin({
      metadata: this.http.get<unknown>(`/q/qadmin/api/entityMetadata/${encodedName}`).pipe(
        catchError(err => of({ error: 'Metadata unavailable' }))
      ),
      data: this.http.get<unknown>(`/q/qadmin/api/data/${encodedName}`).pipe(
        catchError(err => of([]))
      )
    }).pipe(
      map(results => ({
        name,
        metadata: results.metadata,
        data: results.data
      }))
    );
  }

  // Helper for template logic
  hasData(data: unknown): boolean {
    return Array.isArray(data) && data.length > 0;
  }

  getCount(data: unknown): number {
    return Array.isArray(data) ? data.length : 0;
  }
}
