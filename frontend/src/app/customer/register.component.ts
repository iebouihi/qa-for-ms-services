import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { SessionStore } from '../shared/session.store';
import { CustomerApi } from './customer.api';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule],
  template: `
    <section class="panel narrow">
      <h1>Create your account</h1>
      <form [formGroup]="form" (ngSubmit)="submit()" data-testid="register-form" novalidate>
        <label>
          Email
          <input type="email" formControlName="email" autocomplete="email" data-testid="register-email" />
        </label>
        <div class="row">
          <label>
            First name
            <input formControlName="firstName" autocomplete="given-name" data-testid="register-first-name" />
          </label>
          <label>
            Last name
            <input formControlName="lastName" autocomplete="family-name" data-testid="register-last-name" />
          </label>
        </div>
        @if (error()) {
          <p class="error" role="alert" data-testid="register-error">{{ error() }}</p>
        }
        <button type="submit" class="primary" [disabled]="form.invalid || busy()" data-testid="register-submit">
          {{ busy() ? 'Creating…' : 'Create account' }}
        </button>
      </form>
    </section>
  `,
})
export class RegisterComponent {
  private readonly api = inject(CustomerApi);
  private readonly session = inject(SessionStore);
  private readonly router = inject(Router);

  readonly busy = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.api.register(this.form.getRawValue()).subscribe({
      next: (customer) => {
        this.session.signIn(customer);
        this.router.navigateByUrl('/products');
      },
      error: (e: HttpErrorResponse) => {
        this.busy.set(false);
        this.error.set(
          e.status === 409 ? 'This email is already registered.' : 'Registration failed, please try again.',
        );
      },
    });
  }
}
