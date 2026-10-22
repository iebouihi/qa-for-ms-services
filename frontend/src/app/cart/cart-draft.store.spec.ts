import { TestBed } from '@angular/core/testing';
import { Product } from '../shared/models';
import { CartDraftStore, MAX_QUANTITY } from './cart-draft.store';

const product = (id: string, price: number): Product => ({
  id, sku: id, name: id, description: '', price, currency: 'EUR', active: true,
});

describe('CartDraftStore', () => {
  let store: CartDraftStore;

  beforeEach(() => {
    store = TestBed.inject(CartDraftStore);
    store.clear();
  });

  it('adds a product once and increments its quantity afterwards', () => {
    store.add(product('a', 10));
    store.add(product('a', 10));
    store.add(product('b', 1));

    expect(store.lines().map((l) => [l.product.id, l.quantity])).toEqual([['a', 2], ['b', 1]]);
    expect(store.itemCount()).toBe(3);
  });

  it('estimates the total without floating point noise', () => {
    store.add(product('a', 0.1));
    store.add(product('b', 0.2));

    expect(store.estimatedTotal()).toBe(0.3);
  });

  it('caps quantities and removes a line set to zero', () => {
    store.add(product('a', 1));
    store.setQuantity('a', 500);
    expect(store.lines()[0].quantity).toBe(MAX_QUANTITY);

    store.setQuantity('a', 0);
    expect(store.lines()).toEqual([]);
  });
});
