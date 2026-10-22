import { Injectable, computed, signal } from '@angular/core';
import { Product } from '../shared/models';

export const MAX_QUANTITY = 99;

export interface DraftLine {
  product: Product;
  quantity: number;
}

/**
 * Items picked while browsing, before the cart is created server-side.
 * Pure state logic: unit-tested without any HTTP or DOM.
 */
@Injectable({ providedIn: 'root' })
export class CartDraftStore {
  readonly lines = signal<DraftLine[]>([]);

  readonly itemCount = computed(() => this.lines().reduce((n, l) => n + l.quantity, 0));

  /** Display estimate only: the server recomputes the real total from catalog prices. */
  readonly estimatedTotal = computed(() =>
    round2(this.lines().reduce((sum, l) => sum + l.product.price * l.quantity, 0)),
  );

  add(product: Product): void {
    this.lines.update((lines) => {
      const existing = lines.find((l) => l.product.id === product.id);
      if (!existing) {
        return [...lines, { product, quantity: 1 }];
      }
      return lines.map((l) =>
        l === existing ? { ...l, quantity: Math.min(l.quantity + 1, MAX_QUANTITY) } : l,
      );
    });
  }

  setQuantity(productId: string, quantity: number): void {
    if (quantity <= 0) {
      this.remove(productId);
      return;
    }
    this.lines.update((lines) =>
      lines.map((l) =>
        l.product.id === productId ? { ...l, quantity: Math.min(Math.floor(quantity), MAX_QUANTITY) } : l,
      ),
    );
  }

  remove(productId: string): void {
    this.lines.update((lines) => lines.filter((l) => l.product.id !== productId));
  }

  clear(): void {
    this.lines.set([]);
  }
}

function round2(value: number): number {
  return Math.round(value * 100) / 100;
}
