import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ProductPage } from '../shared/models';

@Injectable({ providedIn: 'root' })
export class CatalogApi {
  private readonly http = inject(HttpClient);

  browse(query = '', page = 0, size = 20): Observable<ProductPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (query.trim()) {
      params = params.set('q', query.trim());
    }
    return this.http.get<ProductPage>('/api/products', { params });
  }
}
