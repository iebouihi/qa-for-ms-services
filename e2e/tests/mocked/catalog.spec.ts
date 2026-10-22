import { CATALOG, expect, test } from '../../fixtures/fake-backend';
import { CatalogPage } from '../../pages/shop.pages';

test.describe('Browse products (UI, fake backend)', () => {
  test('lists the catalog and searches it', async ({ page, backend }) => {
    const catalog = new CatalogPage(page);
    await catalog.open();

    await expect(catalog.products()).toHaveCount(CATALOG.length);

    await catalog.search('mouse');
    await expect(catalog.products()).toHaveCount(1);
    await expect(catalog.product('Ergonomic Mouse')).toBeVisible();
  });

  test('shows an empty state when nothing matches', async ({ page, backend }) => {
    const catalog = new CatalogPage(page);
    await catalog.open();

    await catalog.search('sofa');

    await expect(page.getByTestId('catalog-empty')).toBeVisible();
  });

  test('shows an error when product-service is down', async ({ page, backend }) => {
    backend.catalogDown = true;
    await new CatalogPage(page).open();

    await expect(page.getByTestId('catalog-error')).toBeVisible();
  });
});
