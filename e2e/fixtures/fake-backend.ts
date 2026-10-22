import { Page, Route, test as base } from '@playwright/test';

/**
 * A stateful fake of the three services, running inside the browser's network layer.
 * It lets UI tests cover states that are hard to produce on a real environment:
 * catalog down, cart-service not yet aware of a new customer, product withdrawn...
 * Its responses follow the same shapes as the real APIs (contracts/openapi).
 */
export interface FakeProduct {
  id: string;
  sku: string;
  name: string;
  description: string;
  price: number;
  currency: string;
  active: boolean;
}

export const CATALOG: FakeProduct[] = [
  { id: 'p-1', sku: 'SKU-KEYBOARD', name: 'Mechanical Keyboard', description: 'Tenkeyless', price: 89, currency: 'EUR', active: true },
  { id: 'p-2', sku: 'SKU-MOUSE', name: 'Ergonomic Mouse', description: 'Vertical', price: 45.5, currency: 'EUR', active: true },
  { id: 'p-3', sku: 'SKU-WEBCAM', name: 'HD Webcam', description: '1080p', price: 59.99, currency: 'EUR', active: true },
];

export class FakeBackend {
  catalogDown = false;
  /** How many times cart creation answers CUSTOMER_UNKNOWN before succeeding. */
  customerUnknownResponses = 0;
  withdrawnProductIds = new Set<string>();
  readonly registeredEmails = new Set<string>();
  readonly cartRequests: unknown[] = [];

  async install(page: Page) {
    await page.route('**/api/**', (route) => this.handle(route));
  }

  private async handle(route: Route) {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();

    if (url.pathname === '/api/products' && method === 'GET') {
      if (this.catalogDown) {
        return route.fulfill({ status: 503, json: { status: 503, detail: 'down' } });
      }
      const q = (url.searchParams.get('q') ?? '').toLowerCase();
      const content = CATALOG.filter((p) => p.name.toLowerCase().includes(q));
      return route.fulfill({ json: { content, page: 0, size: 20, totalElements: content.length } });
    }

    if (url.pathname === '/api/customers' && method === 'POST') {
      const body = request.postDataJSON();
      if (this.registeredEmails.has(body.email)) {
        return route.fulfill({ status: 409, json: { status: 409, code: 'EMAIL_ALREADY_REGISTERED' } });
      }
      this.registeredEmails.add(body.email);
      return route.fulfill({ status: 201, json: { id: 'c-' + this.registeredEmails.size, ...body } });
    }

    if (url.pathname === '/api/carts' && method === 'POST') {
      const body = request.postDataJSON();
      this.cartRequests.push(body);
      if (this.customerUnknownResponses > 0) {
        this.customerUnknownResponses--;
        return route.fulfill({ status: 422, json: { status: 422, code: 'CUSTOMER_UNKNOWN' } });
      }
      const withdrawn = body.items.find((i: { productId: string }) => this.withdrawnProductIds.has(i.productId));
      if (withdrawn) {
        return route.fulfill({ status: 422, json: { status: 422, code: 'PRODUCT_UNAVAILABLE' } });
      }
      const items = body.items.map((i: { productId: string; quantity: number }) => {
        const p = CATALOG.find((c) => c.id === i.productId)!;
        return { productId: p.id, productName: p.name, unitPrice: p.price, quantity: i.quantity, lineTotal: p.price * i.quantity };
      });
      const total = Math.round(items.reduce((s: number, i: { lineTotal: number }) => s + i.lineTotal, 0) * 100) / 100;
      return route.fulfill({
        status: 201,
        json: { id: 'cart-' + this.cartRequests.length, customerId: body.customerId, currency: 'EUR', total, items },
      });
    }

    return route.fulfill({ status: 404, json: { status: 404 } });
  }
}

/** `test` with a ready-to-use fake backend, installed before the page loads. */
export const test = base.extend<{ backend: FakeBackend }>({
  backend: async ({ page }, use) => {
    const backend = new FakeBackend();
    await backend.install(page);
    await use(backend);
  },
});

export { expect } from '@playwright/test';
