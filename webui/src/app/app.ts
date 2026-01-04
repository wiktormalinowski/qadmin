import {Component, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {forkJoin, map, Observable, of, switchMap} from 'rxjs';
import {AsyncPipe, JsonPipe} from '@angular/common';

@Component({
  selector: 'app-root',
  imports: [
    AsyncPipe,
    JsonPipe
  ],
  template: `
    <h1>Available entities:</h1>
    <ul>
      @for (entity of (entities$ | async); track $index) {
        <li>
          <strong>{{ entity }}</strong>

          @if (entitiesMetadata$ | async; as meta) {
            <pre>{{ meta[entity] | json }}</pre>
          } @else {
            <p>Loading metadata...</p>
          }
        </li>
      } @empty {
        <li>There are no entities!</li>
      }
    </ul>
  `
})
export class App implements OnInit {
  entities$: Observable<string[]> | undefined;
  entitiesMetadata$: Observable<Record<string, unknown>> | undefined;

  constructor(private http: HttpClient) {
  }

  ngOnInit() {
    this.entities$ = this.http.get<string[]>('/q/qadmin/api/entities')
    this.entitiesMetadata$ = this.entities$.pipe(
      switchMap(entities => {
        if (!entities || entities.length === 0) {
          return of({} as Record<string, unknown>);
        }

        const requests = entities.map(entity =>
          this.http
            .get<unknown>(`/q/qadmin/api/entityMetadata/${encodeURIComponent(entity)}`)
            .pipe(map(res => ({ entity, res })))
        );

        return forkJoin(requests).pipe(
          map(results =>
            results.reduce((acc, { entity, res }) => {
              acc[entity] = res;
              return acc;
            }, {} as Record<string, unknown>)
          )
        );
      })
    );
  }
}
