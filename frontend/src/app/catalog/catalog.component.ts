import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CartDraftStore } from '../cart/cart-draft.store';
import { Product } from '../shared/models';
import { CatalogApi } from './catalog.api';

@Component({
  selector: 'app-catalog',
  imports: [FormsModule, CurrencyPipe],
  template: `
    <section class="panel">
      <div class="toolbar">
        <h1>Products</h1>
        <form (ngSubmit)="search()" class="search" role="search">
          <input
            [(ngModel)]="query"
            name="q"
            placeholder="Search products"
            aria-label="Search products"
            data-testid="catalog-search"
          />
          <button type="submit" data-testid="catalog-search-submit">Search</button>
        </form>
      </div>

      @if (error()) {
        <p class="error" role="alert" data-testid="catalog-error">Products could not be loaded.</p>
      } @else if (loading()) {
        <p class="muted">Loading…</p>
      } @else if (products().length === 0) {
        <p class="muted" data-testid="catalog-empty">No product matches your search.</p>
      } @else {
        <ul class="grid" data-testid="catalog-list">
          @for (product of products(); track product.id) {
            <li class="card" data-testid="product-card" [attr.data-product-id]="product.id">
              <h2 data-testid="product-name">{{ product.name }}</h2>
              <p class="muted">{{ product.description }}</p>
              <div class="card-footer">
                <strong data-testid="product-price">{{ product.price | currency: product.currency }}</strong>
                <button class="primary" (click)="add(product)" data-testid="add-to-cart">Add to cart</button>
              </div>
            </li>
          }
        </ul>
      }
    </section>
  `,
})
export class CatalogComponent implements OnInit {
  private readonly api = inject(CatalogApi);
  private readonly cart = inject(CartDraftStore);

  query = '';
  readonly products = signal<Product[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);

  ngOnInit(): void {
    this.search();
  }

  search(): void {
    this.loading.set(true);
    this.error.set(false);
    this.api.browse(this.query).subscribe({
      next: (page) => {
        this.products.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  add(product: Product): void {
    this.cart.add(product);
  }
}
