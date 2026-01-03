import {Component, OnInit, signal} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {HttpClient} from '@angular/common/http';

@Component({
  selector: 'app-root',
  template: `
    <h1>Available entities:</h1>
    <ul>
      @for (entity of entities; track entity) {
        <li>{{ entity }}</li>
      } @empty {
        <li>There are no enitites.</li>
      }
    </ul>
  `
})
export class AppComponent implements OnInit {
  entities: string[] = [];
  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.http.get<string[]>('/q/qadmin/api/entities')
      .subscribe(data => this.entities = data);
  }
}
