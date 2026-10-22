import { Injectable, signal } from '@angular/core';
import { Customer } from './models';

const KEY = 'shop.customer';

/** The registered customer for this browser (a stand-in for real authentication). */
@Injectable({ providedIn: 'root' })
export class SessionStore {
  readonly customer = signal<Customer | null>(read());

  signIn(customer: Customer): void {
    this.customer.set(customer);
    try {
      localStorage.setItem(KEY, JSON.stringify(customer));
    } catch {
      /* storage unavailable: session lives in memory only */
    }
  }

  signOut(): void {
    this.customer.set(null);
    try {
      localStorage.removeItem(KEY);
    } catch {
      /* ignore */
    }
  }
}

function read(): Customer | null {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as Customer) : null;
  } catch {
    return null;
  }
}
