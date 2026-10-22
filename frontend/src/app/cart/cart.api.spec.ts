import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { CART_RETRY_POLICY, CartApi } from './cart.api';

const request = { customerId: 'c1', items: [{ productId: 'p1', quantity: 1 }] };
const tick = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('CartApi', () => {
  let api: CartApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: CART_RETRY_POLICY, useValue: { attempts: 3, delayMs: 0 } },
      ],
    });
    api = TestBed.inject(CartApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('retries while the customer is not yet known (eventual consistency)', async () => {
    const result = firstValueFrom(api.create(request));

    http.expectOne('/api/carts').flush({ code: 'CUSTOMER_UNKNOWN' }, { status: 422, statusText: 'Unprocessable' });
    await tick();
    http.expectOne('/api/carts').flush({ id: 'cart-1', total: 10 });

    await expect(result).resolves.toMatchObject({ id: 'cart-1' });
  });

  it('does not retry business errors', async () => {
    const result = firstValueFrom(api.create(request));

    http.expectOne('/api/carts').flush({ code: 'PRODUCT_UNAVAILABLE' }, { status: 422, statusText: 'Unprocessable' });

    await expect(result).rejects.toMatchObject({ status: 422 });
  });
});
