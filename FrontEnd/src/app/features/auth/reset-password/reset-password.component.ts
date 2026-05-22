import { CommonModule } from '@angular/common';
import { Component, computed, inject, Input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss',
})
export class ResetPasswordComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Vinculado al query param `?token=...` por withComponentInputBinding(). */
  @Input() token?: string;

  protected readonly loading = signal(false);
  protected readonly success = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly missingToken = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    // Mismo mínimo que el registro (6 caracteres) para consistencia con el back.
    newPassword: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]],
  });

  /** Tick reactivo para que los computeds reaccionen a cambios del form. */
  private readonly formTick = signal(0);

  protected readonly passwordsMatch = computed(() => {
    this.formTick();
    const a = this.form.controls.newPassword.value;
    const b = this.form.controls.confirmPassword.value;
    return a === b;
  });

  /** True si el usuario ya ha escrito algo en confirmación pero no coincide. */
  protected readonly showMismatch = computed(() => {
    this.formTick();
    const b = this.form.controls.confirmPassword.value;
    return !!b && !this.passwordsMatch();
  });

  ngOnInit(): void {
    if (!this.token) {
      this.missingToken.set(true);
    }
    // Refresca los computeds (passwordsMatch, showMismatch) en cada cambio.
    this.form.valueChanges.subscribe(() => this.formTick.update((n) => n + 1));
  }

  submit(): void {
    if (this.form.invalid || !this.token) return;
    if (!this.passwordsMatch()) {
      this.errorMessage.set('Las contraseñas no coinciden.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.auth.resetPassword(this.token, this.form.controls.newPassword.value).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
        setTimeout(() => this.router.navigate(['/login']), 2500);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(
          err?.error?.message ?? 'No se pudo restablecer la contraseña. Solicita un enlace nuevo.',
        );
      },
    });
  }
}
