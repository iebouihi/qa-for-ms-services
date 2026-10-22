import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Customer } from '../shared/models';

export interface RegisterCustomer {
  email: string;
  firstName: string;
  lastName: string;
}

@Injectable({ providedIn: 'root' })
export class CustomerApi {
  private readonly http = inject(HttpClient);

  register(request: RegisterCustomer): Observable<Customer> {
    return this.http.post<Customer>('/api/customers', request);
  }
}
