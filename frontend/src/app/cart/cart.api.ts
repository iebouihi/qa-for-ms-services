import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, InjectionToken, inject } from '@angular/core';
import { Observable, retry, throwError, timer } from 'rxjs';
import { Cart } from '../shared/models';

export interface CreateCart {
  customerId: string;
  items: { productId: string; quantity: number }[];
}

export interface RetryPolicy {
  attempts: number;
  delayMs: number;
}

/** Overridable in tests (delay 0) and per environment. */
export const CART_RETRY_POLICY = new InjectionToken<RetryPolicy>('CART_RETRY_POLICY', {
  factory: () => ({ attempts: 5, delayMs: 400 }),
});

@Injectable({ providedIn: 'root' })
export class CartApi {
  private readonly http = inject(HttpClient);
  private readonly policy = inject(CART_RETRY_POLICY);

  /**
   * Right after registration, cart-service may not have consumed the CustomerRegistered event
   * yet (eventual consistency). That specific answer is retried with a growing delay;
   * every other error is surfaced immediately.
   */
  create(request: CreateCart): Observable<Cart> {
    return this.http.post<Cart>('/api/carts', request).pipe(
      retry({
        count: this.policy.attempts,
        delay: (error: unknown, attempt: number) =>
          isCustomerNotYetKnown(error) ? timer(this.policy.delayMs * attempt) : throwError(() => error),
      }),
    );
  }
}

export function isCustomerNotYetKnown(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === 422 && error.error?.code === 'CUSTOMER_UNKNOWN';
}
