import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { environment } from '../../../../environments/environment';
import {
  emailWithTldValidator,
  spanishPhoneValidator,
  spanishTaxIdValidator,
} from '../../../core/validators/spanish.validators';

type RegisterRole = 'COLLECTOR' | 'FOUNDATION' | 'TRANSPORT';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  /** Vinculado al query param `?role=...` mediante withComponentInputBinding(). */
  @Input() role?: string;

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly success = signal(false);
  protected file: File | null = null;
  /** Marcamos si el usuario ha intentado enviar para mostrar el error del archivo. */
  protected readonly submitted = signal(false);

  protected readonly roleOptions = [
    { value: 'COLLECTOR', label: 'Coleccionista' },
    { value: 'FOUNDATION', label: 'Museo / Fundación' },
    { value: 'TRANSPORT', label: 'Transporte' },
  ] as const;

  /**
   * Todos los campos son obligatorios. La contraseña debe tener al menos 6
   * caracteres (el back exige también 6). Email, teléfono y DNI/CIF/LEI
   * tienen validaciones específicas.
   */
  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email, emailWithTldValidator()]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    phone: ['', [Validators.required, spanishPhoneValidator()]],
    taxId: ['', [Validators.required, spanishTaxIdValidator()]],
    role: ['COLLECTOR' as RegisterRole, [Validators.required]],
  });

  ngOnInit(): void {
    if (this.role && this.isValidRole(this.role)) {
      this.form.controls.role.setValue(this.role);
    }
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.file = input.files?.[0] ?? null;
  }

  submit(): void {
    this.submitted.set(true);
    // Si el formulario es inválido, mostramos los errores en cada campo.
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.file) return;

    this.loading.set(true);
    this.errorMessage.set(null);
    this.success.set(false);

    const data = new FormData();
    data.append(
      'data',
      new Blob([JSON.stringify(this.form.getRawValue())], { type: 'application/json' }),
    );
    data.append('file', this.file);

    this.http.post(`${environment.apiBaseUrl}/auth/register`, data).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
        setTimeout(() => this.router.navigate(['/login']), 2500);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo completar el registro.');
      },
    });
  }

  /** Helper para el template: ¿hay que mostrar error en este campo? */
  showError(name: keyof typeof this.form.controls): boolean {
    const ctrl = this.form.controls[name];
    return ctrl.invalid && (ctrl.touched || ctrl.dirty || this.submitted());
  }

  private isValidRole(value: string): value is RegisterRole {
    return value === 'COLLECTOR' || value === 'FOUNDATION' || value === 'TRANSPORT';
  }
}
