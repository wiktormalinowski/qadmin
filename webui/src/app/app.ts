import { Component, inject } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { EntityService } from '../entity/entity.service';
import { EntityCardComponent } from '../entity-card/entity-card.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [AsyncPipe, EntityCardComponent],
  templateUrl: 'app.html',
  styleUrls: ['app.css']
})
export class App {
  private service = inject(EntityService);

  readonly vm$ = this.service.entities$;
}
