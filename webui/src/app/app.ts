import { Component, inject } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { EntityService } from '../entity/entity.service';
import { EntityCardComponent } from '../entity-card/entity-card.component';
import {Observable} from 'rxjs';
import {EntityViewModel} from '../entity/entity.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [AsyncPipe, EntityCardComponent],
  templateUrl: 'app.html',
  styleUrls: ['app.css']
})
export class App {
  protected service = inject(EntityService);

  readonly vm$ = this.service.entities$;
  protected entityFromPrompt$: Observable<EntityViewModel> | undefined;
}
