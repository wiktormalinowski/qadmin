import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EntityViewModel } from './entity.model';

@Injectable({ providedIn: 'root' })
export class EntityService {
  private http = inject(HttpClient);

  public getEntityFromPrompt(name: string): Observable<EntityViewModel> {
    return this.http.post<EntityViewModel>('/q/qadmin/api/ai/query', {prompt: name});
  }

  public deleteEntity(name: string, id: any): Observable<void> {
    const encodedName = encodeURIComponent(name);
    const encodedId = encodeURIComponent(String(id));
    return this.http.delete<void>(`/q/qadmin/api/entities/${encodedName}/${encodedId}`);
  }
}
