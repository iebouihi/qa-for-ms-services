import { expect, test } from '@playwright/test';
import { CartPage, CatalogPage, RegisterPage } from '../../pages/shop.pages';

/**
 * The 3 business journeys on a real deployed stack: Angular -> gateway -> 3 services,
 * PostgreSQL and Kafka. Deliberately few and happy-path only: they prove the pieces are
 * wired together; behaviour details are covered by faster layers below.
 * Each run creates its own customer, so journeys can run in parallel on a shared environment.
 */
test.describe('Shopping journeys (full stack)', () => {
  test('a visitor browses the catalog', async ({ page }) => {
    const catalog = new CatalogPage(page);
    await catalog.open();

    await expect(catalog.products().first()).toBeVisible();
    await catalog.search('keyboard');
    await expect(catalog.products()).not.toHaveCount(0);
  });

  test('a visitor registers, picks products and creates a cart', async ({ page }) => {
    const register = new RegisterPage(page);
    await register.open();
    await register.register({
      email: `test-${crypto.randomUUID().slice(0, 8)}@example.test`,
      firstName: 'Journey',
      lastName: 'Tester',
    });
    await expect(page.getByTestId('signed-in-as')).toHaveText('Journey');

    const catalog = new CatalogPage(page);
    await catalog.addFirstProductToCart();
    await expect(catalog.cartCount()).toHaveText('1');

    const cart = new CartPage(page);
    await cart.open();
    await cart.createCart();

    await expect(cart.confirmation()).toBeVisible({ timeout: 20_000 });
    await expect(page.getByTestId('cart-id')).not.toBeEmpty();
  });
});
