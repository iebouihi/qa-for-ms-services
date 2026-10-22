import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CartDraftStore } from '../cart/cart-draft.store';
import { CatalogComponent } from './catalog.component';

describe('CatalogComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CatalogComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    TestBed.inject(CartDraftStore).clear();
  });

  it('lists products from the API and adds one to the cart', async () => {
    const fixture = TestBed.createComponent(CatalogComponent);
    fixture.detectChanges();

    TestBed.inject(HttpTestingController)
      .expectOne((r) => r.url === '/api/products')
      .flush({
        content: [{ id: 'p1', sku: 'S1', name: 'Mechanical Keyboard', description: '', price: 89, currency: 'EUR', active: true }],
        page: 0, size: 20, totalElements: 1,
      });
    await fixture.whenStable();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll('[data-testid="product-card"]').length).toBe(1);
    expect(el.querySelector('[data-testid="product-name"]')?.textContent).toContain('Mechanical Keyboard');

    (el.querySelector('[data-testid="add-to-cart"]') as HTMLButtonElement).click();
    expect(TestBed.inject(CartDraftStore).itemCount()).toBe(1);
  });

  it('shows an error state when the catalog is down', async () => {
    const fixture = TestBed.createComponent(CatalogComponent);
    fixture.detectChanges();

    TestBed.inject(HttpTestingController)
      .expectOne((r) => r.url === '/api/products')
      .flush(null, { status: 503, statusText: 'Unavailable' });
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="catalog-error"]')).toBeTruthy();
  });
});
