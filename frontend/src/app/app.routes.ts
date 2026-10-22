import { Routes } from '@angular/router';

/**
 * One lazy-loaded area per business domain (customer, catalog, cart). Each area only
 * talks to its own backend service, so it can later be split into a separately deployed
 * micro-frontend (e.g. Native Federation) without changing the tests.
 */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'products' },
  {
    path: 'register',
    title: 'Register · Headless Shop',
    loadComponent: () => import('./customer/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'products',
    title: 'Products · Headless Shop',
    loadComponent: () => import('./catalog/catalog.component').then((m) => m.CatalogComponent),
  },
  {
    path: 'cart',
    title: 'Cart · Headless Shop',
    loadComponent: () => import('./cart/cart.component').then((m) => m.CartComponent),
  },
  { path: '**', redirectTo: 'products' },
];
