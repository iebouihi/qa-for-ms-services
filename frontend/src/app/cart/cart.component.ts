import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Cart } from '../shared/models';
import { SessionStore } from '../shared/session.store';
import { CartApi } from './cart.api';
import { CartDraftStore, MAX_QUANTITY } from './cart-draft.store';

const MESSAGES: Record<string, string> = {
  CUSTOMER_UNKNOWN: 'Your account is still being set up. Please try again in a moment.',
  PRODUCT_UNAVAILABLE: 'One of the products is no longer available.',
  PRODUCT_SERVICE_UNAVAILABLE: 'The catalog is temporarily unavailable. Please try again.',
};

@Component({
  selector: 'app-cart',
  imports: [CurrencyPipe, RouterLink],
  template: `
    <section class="panel narrow">
      <h1>Your cart</h1>

      @if (created(); as cart) {
        <div class="success" data-testid="cart-confirmation">
          <p>Cart created.</p>
          <p>Reference <code data-testid="cart-id">{{ cart.id }}</code></p>
          <p>Total <strong data-testid="cart-total">{{ cart.total | currency: cart.currency }}</strong></p>
          <a routerLink="/products">Continue shopping</a>
        </div>
      } @else if (draft.lines().length === 0) {
        <p class="muted" data-testid="cart-empty">Your cart is empty. <a routerLink="/products">Browse products</a></p>
      } @else {
        <ul class="lines" data-testid="cart-lines">
          @for (line of draft.lines(); track line.product.id) {
            <li data-testid="cart-line">
              <span class="grow" data-testid="cart-line-name">{{ line.product.name }}</span>
              <input
                type="number"
                min="1"
                [max]="max"
                [value]="line.quantity"
                (change)="setQuantity(line.product.id, $event)"
                [attr.aria-label]="'Quantity of ' + line.product.name"
                data-testid="cart-line-quantity"
              />
              <span>{{ line.product.price * line.quantity | currency: line.product.currency }}</span>
              <button (click)="draft.remove(line.product.id)" aria-label="Remove">✕</button>
            </li>
          }
        </ul>
        <p class="total">Estimated total <strong data-testid="cart-estimated-total">{{ draft.estimatedTotal() | currency: currency() }}</strong></p>

        @if (!session.customer()) {
          <p class="muted" data-testid="cart-login-required">
            <a routerLink="/register">Create an account</a> to save your cart.
          </p>
        }
        @if (error()) {
          <p class="error" role="alert" data-testid="cart-error">{{ error() }}</p>
        }
        <button class="primary" [disabled]="!session.customer() || busy()" (click)="createCart()" data-testid="create-cart">
          {{ busy() ? 'Creating…' : 'Create cart' }}
        </button>
      }
    </section>
  `,
})
export class CartComponent {
  readonly draft = inject(CartDraftStore);
  readonly session = inject(SessionStore);
  private readonly api = inject(CartApi);

  readonly max = MAX_QUANTITY;
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly created = signal<Cart | null>(null);
  readonly currency = computed(() => this.draft.lines()[0]?.product.currency ?? 'EUR');

  setQuantity(productId: string, event: Event): void {
    this.draft.setQuantity(productId, Number((event.target as HTMLInputElement).value));
  }

  createCart(): void {
    const customer = this.session.customer();
    if (!customer) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.api
      .create({
        customerId: customer.id,
        items: this.draft.lines().map((l) => ({ productId: l.product.id, quantity: l.quantity })),
      })
      .subscribe({
        next: (cart) => {
          this.created.set(cart);
          this.draft.clear();
          this.busy.set(false);
        },
        error: (e: HttpErrorResponse) => {
          this.busy.set(false);
          this.error.set(MESSAGES[e.error?.code] ?? 'The cart could not be created. Please try again.');
        },
      });
  }
}
