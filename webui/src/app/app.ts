import {Component, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {AsyncPipe} from '@angular/common';

@Component({
  selector: 'app-root',
  imports: [
    AsyncPipe
  ],
  template: `
    <h1>Available entities:</h1>
    <ul>
      @for (entity of (entities$ | async); track $index) {
        <li>{{ entity }}</li>
      } @empty {
        <li>There are no enitites!!!</li>
      }
    </ul>
  `
})
export class App implements OnInit {
  entities$: Observable<string[]> | undefined;

  constructor(private http: HttpClient) {
  }

  ngOnInit() {
    this.entities$ = this.http.get<string[]>('/q/qadmin/api/entities');
  }
}
