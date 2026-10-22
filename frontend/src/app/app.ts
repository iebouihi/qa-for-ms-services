import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CartDraftStore } from './cart/cart-draft.store';
import { SessionStore } from './shared/session.store';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <a routerLink="/products" class="brand">Headless Shop</a>
      <nav>
        <a routerLink="/products" routerLinkActive="active">Products</a>
        <a routerLink="/cart" routerLinkActive="active" data-testid="nav-cart">
          Cart <span class="badge" data-testid="cart-count">{{ cart.itemCount() }}</span>
        </a>
        @if (session.customer(); as customer) {
          <span class="who" data-testid="signed-in-as">{{ customer.firstName }}</span>
          <button class="link" (click)="session.signOut()" data-testid="sign-out">Sign out</button>
        } @else {
          <a routerLink="/register" routerLinkActive="active" data-testid="nav-register">Register</a>
        }
      </nav>
    </header>
    <main>
      <router-outlet />
    </main>
  `,
})
export class App {
  readonly session = inject(SessionStore);
  readonly cart = inject(CartDraftStore);
}
