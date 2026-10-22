import { Locator, Page, expect } from '@playwright/test';

/**
 * Page objects: tests speak business language, selectors live here only.
 * Selectors use data-testid (a frontend standard), never CSS classes or text that designers change.
 */
export class RegisterPage {
  constructor(private readonly page: Page) {}

  async open() {
    await this.page.goto('/register');
  }

  async register(customer: { email: string; firstName: string; lastName: string }) {
    await this.page.getByTestId('register-email').fill(customer.email);
    await this.page.getByTestId('register-first-name').fill(customer.firstName);
    await this.page.getByTestId('register-last-name').fill(customer.lastName);
    await this.page.getByTestId('register-submit').click();
  }

  error(): Locator {
    return this.page.getByTestId('register-error');
  }
}

export class CatalogPage {
  constructor(private readonly page: Page) {}

  async open() {
    await this.page.goto('/products');
  }

  products(): Locator {
    return this.page.getByTestId('product-card');
  }

  product(name: string): Locator {
    return this.products().filter({ has: this.page.getByTestId('product-name').getByText(name, { exact: true }) });
  }

  async search(text: string) {
    await this.page.getByTestId('catalog-search').fill(text);
    await this.page.getByTestId('catalog-search-submit').click();
  }

  async addToCart(name: string) {
    await this.product(name).getByTestId('add-to-cart').click();
  }

  async addFirstProductToCart(): Promise<string> {
    const first = this.products().first();
    const name = (await first.getByTestId('product-name').textContent())!.trim();
    await first.getByTestId('add-to-cart').click();
    return name;
  }

  cartCount(): Locator {
    return this.page.getByTestId('cart-count');
  }
}

export class CartPage {
  constructor(private readonly page: Page) {}

  async open() {
    await this.page.getByTestId('nav-cart').click();
    await expect(this.page).toHaveURL(/\/cart$/);
  }

  lines(): Locator {
    return this.page.getByTestId('cart-line');
  }

  async setQuantity(productName: string, quantity: number) {
    const input = this.lines().filter({ hasText: productName }).getByTestId('cart-line-quantity');
    await input.fill(String(quantity));
    await input.blur();
  }

  async createCart() {
    await this.page.getByTestId('create-cart').click();
  }

  confirmation(): Locator {
    return this.page.getByTestId('cart-confirmation');
  }

  total(): Locator {
    return this.page.getByTestId('cart-total');
  }

  error(): Locator {
    return this.page.getByTestId('cart-error');
  }
}
