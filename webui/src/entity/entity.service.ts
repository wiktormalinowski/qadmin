import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, forkJoin, map, Observable, of, shareReplay, switchMap } from 'rxjs';
import { EntityViewModel } from './entity.model';

@Injectable({ providedIn: 'root' })
export class EntityService {
  private http = inject(HttpClient);

  /**
   * Strumień pobierający listę nazw encji, a następnie równolegle ich szczegóły.
   * Wynik jest cachowany (replay: 1).
   */
  readonly entities$: Observable<EntityViewModel[]> = this.http.get<string[]>('/q/qadmin/api/entities').pipe(
    switchMap(entityNames => {
      if (!entityNames?.length) return of([]);
      return forkJoin(entityNames.map(name => this.fetchEntityDetails(name)));
    }),
    catchError(err => {
      console.error('Critical failure loading entities', err);
      return of([]);
    }),
    shareReplay(1)
  );

  /**
   * Pobiera metadane i dane dla konkretnej encji w sposób izolowany (błąd nie przerywa całości).
   */
  private fetchEntityDetails(name: string): Observable<EntityViewModel> {
    const encodedName = encodeURIComponent(name);

    return forkJoin({
      metadata: this.http.get<unknown>(`/q/qadmin/api/entityMetadata/${encodedName}`).pipe(
        catchError(() => of({ error: 'Metadata unavailable' }))
      ),
      data: this.http.get<unknown>(`/q/qadmin/api/data/${encodedName}`).pipe(
        catchError(() => of([]))
      )
    }).pipe(
      map(res => ({ name, metadata: res.metadata, data: res.data }))
    );
  }
}
