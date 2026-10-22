import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { SessionStore } from './shared/session.store';

describe('App shell', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('offers registration when nobody is signed in', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('[data-testid="nav-register"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="cart-count"]')?.textContent?.trim()).toBe('0');
  });

  it('greets the signed-in customer', async () => {
    TestBed.inject(SessionStore).signIn({ id: 'c1', email: 'jane@example.com', firstName: 'Jane', lastName: 'Doe' });
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('[data-testid="signed-in-as"]')?.textContent).toContain('Jane');
  });
});
