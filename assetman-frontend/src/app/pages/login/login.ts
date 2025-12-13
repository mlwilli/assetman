import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../core/auth/auth.service';

// Material
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';

type LoginForm = {
  tenantSlug: FormControl<string>;
  email: FormControl<string>;
  password: FormControl<string>;
};

@Component({
  standalone: true,
  selector: 'app-login-page',
  templateUrl: './login.html',
  styleUrls: ['./login.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressBarModule,
  ],
})
export class LoginPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  loading = false;
  errorMessage: string | null = null;

  readonly form: FormGroup<LoginForm> = this.fb.nonNullable.group({
    tenantSlug: this.fb.nonNullable.control('', { validators: [Validators.required] }),
    email: this.fb.nonNullable.control('', { validators: [Validators.required, Validators.email] }),
    password: this.fb.nonNullable.control('', { validators: [Validators.required] }),
  });

  get controls() {
    return this.form.controls;
  }

  submit(): void {
    this.errorMessage = null;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;

    const { tenantSlug, email, password } = this.form.getRawValue();

    this.auth.login({ tenantSlug, email, password }).subscribe({
      next: () => {
        this.loading = false;
        void this.router.navigateByUrl('/dashboard');
      },
      error: (err: unknown) => {
        this.loading = false;

        if (err instanceof HttpErrorResponse) {
          if (err.status === 401) {
            this.errorMessage = 'Invalid tenant slug, email, or password.';
            return;
          }
          if (err.status === 400) {
            this.errorMessage = 'Please check the form fields and try again.';
            return;
          }
          if (err.status === 0) {
            this.errorMessage = 'Cannot reach the server. Is the backend running?';
            return;
          }
        }

        this.errorMessage = 'Login failed. Please try again.';
      },
    });
  }
}
