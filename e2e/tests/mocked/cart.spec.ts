import { Page } from '@playwright/test';
import { expect, test } from '../../fixtures/fake-backend';
import { CartPage, CatalogPage, RegisterPage } from '../../pages/shop.pages';

async function signInAndPick(page: Page, ...products: string[]) {
  const register = new RegisterPage(page);
  await register.open();
  await register.register({ email: `buyer-${Date.now()}@example.test`, firstName: 'Buyer', lastName: 'Test' });
  const catalog = new CatalogPage(page);
  for (const name of products) {
    await catalog.addToCart(name);
  }
  return catalog;
}

test.describe('Create cart (UI, fake backend)', () => {
  test('creates a cart with the selected products and quantities', async ({ page, backend }) => {
    const catalog = await signInAndPick(page, 'Mechanical Keyboard', 'Ergonomic Mouse');
    await expect(catalog.cartCount()).toHaveText('2');

    const cart = new CartPage(page);
    await cart.open();
    await cart.setQuantity('Ergonomic Mouse', 2);
    await cart.createCart();

    await expect(cart.confirmation()).toBeVisible();
    await expect(cart.total()).toHaveText('€180.00'); // 89 + 2 x 45.50
    expect(backend.cartRequests[0]).toMatchObject({
      items: [{ productId: 'p-1', quantity: 1 }, { productId: 'p-2', quantity: 2 }],
    });
  });

  test('transparently retries while cart-service has not yet received the new customer', async ({ page, backend }) => {
    backend.customerUnknownResponses = 2; // eventual consistency, simulated deterministically
    await signInAndPick(page, 'HD Webcam');

    const cart = new CartPage(page);
    await cart.open();
    await cart.createCart();

    await expect(cart.confirmation()).toBeVisible();
    expect(backend.cartRequests).toHaveLength(3);
  });

  test('explains when a product was withdrawn meanwhile', async ({ page, backend }) => {
    backend.withdrawnProductIds.add('p-3');
    await signInAndPick(page, 'HD Webcam');

    const cart = new CartPage(page);
    await cart.open();
    await cart.createCart();

    await expect(cart.error()).toHaveText('One of the products is no longer available.');
  });

  test('requires an account before creating a cart', async ({ page, backend }) => {
    const catalog = new CatalogPage(page);
    await catalog.open();
    await catalog.addToCart('HD Webcam');

    await new CartPage(page).open();

    await expect(page.getByTestId('cart-login-required')).toBeVisible();
    await expect(page.getByTestId('create-cart')).toBeDisabled();
  });
});
