import { expect, test } from '../../fixtures/fake-backend';
import { RegisterPage } from '../../pages/shop.pages';

test.describe('Customer registration (UI, fake backend)', () => {
  test('registers and signs the customer in', async ({ page, backend }) => {
    const register = new RegisterPage(page);
    await register.open();

    await register.register({ email: 'jane@example.test', firstName: 'Jane', lastName: 'Doe' });

    await expect(page).toHaveURL(/\/products$/);
    await expect(page.getByTestId('signed-in-as')).toHaveText('Jane');
  });

  test('explains when the email is already registered', async ({ page, backend }) => {
    backend.registeredEmails.add('taken@example.test');
    const register = new RegisterPage(page);
    await register.open();

    await register.register({ email: 'taken@example.test', firstName: 'Jane', lastName: 'Doe' });

    await expect(register.error()).toHaveText('This email is already registered.');
  });

  test('cannot submit an invalid form', async ({ page, backend }) => {
    await new RegisterPage(page).open();

    await page.getByTestId('register-email').fill('not-an-email');

    await expect(page.getByTestId('register-submit')).toBeDisabled();
  });
});
