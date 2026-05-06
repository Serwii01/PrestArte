import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { environment } from '../../../../environments/environment';

type RegisterRole = 'COLLECTOR' | 'FOUNDATION' | 'TRANSPORT';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="min-h-screen flex flex-row bg-white">
      <!-- Panel izquierdo con imagen -->
      <div class="hidden lg:flex lg:w-1/2 relative bg-gray-900">
        <div
          class="absolute inset-0 bg-cover bg-center opacity-90"
          style="background-image: url('https://images.unsplash.com/photo-1531913764164-f85c52e6e654?auto=format&fit=crop&w=1600&q=80');"
        ></div>
        <div class="absolute inset-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent"></div>
        <div class="relative z-10 flex flex-col justify-end p-16 text-white w-full">
          <span class="material-symbols-outlined text-white mb-6" style="font-size:40px;">museum</span>
          <h1 class="text-4xl font-extrabold tracking-tight mb-4">
            Únete a la red profesional del préstamo de arte
          </h1>
          <p class="text-lg text-gray-300 max-w-md leading-relaxed">
            Tras registrarte, un administrador revisará tu documentación y te dará acceso completo a la plataforma.
          </p>
        </div>
      </div>

      <!-- Panel derecho: formulario -->
      <div class="flex flex-1 flex-col justify-center items-center px-4 py-12 sm:px-6 lg:px-20 xl:px-24 overflow-y-auto">
        <div class="w-full max-w-md space-y-6">
          <div class="lg:hidden flex justify-center mb-2">
            <span class="material-symbols-outlined text-primary" style="font-size:40px;">museum</span>
          </div>

          <div class="text-center lg:text-left">
            <h2 class="text-text-main tracking-tight text-[28px] font-bold leading-tight pb-2">
              Crear cuenta
            </h2>
            <p class="text-text-secondary text-sm">
              Tu cuenta quedará pendiente de aprobación por un administrador.
            </p>
          </div>

          <!-- Selector de tipo de cuenta (estilo Stitch) -->
          <div>
            <label class="text-xs font-bold uppercase tracking-wider text-text-secondary mb-2 block">
              Tipo de cuenta
            </label>
            <div class="flex p-1 rounded-lg bg-gray-soft">
              @for (opt of roleOptions; track opt.value) {
                <button
                  type="button"
                  (click)="form.controls.role.setValue(opt.value)"
                  class="flex-1 py-2.5 text-sm font-medium rounded-md transition-all"
                  [class.bg-white]="form.controls.role.value === opt.value"
                  [class.text-text-main]="form.controls.role.value === opt.value"
                  [class.shadow-sm]="form.controls.role.value === opt.value"
                  [class.text-text-secondary]="form.controls.role.value !== opt.value"
                >
                  {{ opt.label }}
                </button>
              }
            </div>
          </div>

          <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-4">
            <label class="block">
              <p class="text-text-main text-sm font-medium pb-2">Nombre completo</p>
              <input formControlName="name"
                class="w-full rounded-lg border border-border bg-white h-11 px-4 text-base
                       focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary" />
            </label>

            <label class="block">
              <p class="text-text-main text-sm font-medium pb-2">Email</p>
              <input type="email" formControlName="email"
                class="w-full rounded-lg border border-border bg-white h-11 px-4 text-base
                       focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary" />
            </label>

            <div class="grid grid-cols-2 gap-3">
              <label class="block">
                <p class="text-text-main text-sm font-medium pb-2">Contraseña</p>
                <input type="password" formControlName="password"
                  class="w-full rounded-lg border border-border bg-white h-11 px-4 text-base
                         focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary" />
              </label>
              <label class="block">
                <p class="text-text-main text-sm font-medium pb-2">Teléfono</p>
                <input formControlName="phone"
                  class="w-full rounded-lg border border-border bg-white h-11 px-4 text-base
                         focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary" />
              </label>
            </div>

            <label class="block">
              <p class="text-text-main text-sm font-medium pb-2">DNI / CIF / LEI</p>
              <input formControlName="taxId"
                class="w-full rounded-lg border border-border bg-white h-11 px-4 text-base
                       focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary" />
            </label>

            <!-- Upload de documento de verificación (estilo Stitch dropzone) -->
            <label class="block">
              <p class="text-text-main text-sm font-medium pb-2">
                Documento de verificación
                <span class="text-text-secondary font-normal">(PDF, JPG o PNG, máx. 10 MB)</span>
              </p>
              <label
                class="flex items-center justify-center gap-3 h-24 rounded-lg border-2 border-dashed
                       border-border hover:border-primary/50 hover:bg-primary/5 cursor-pointer transition-colors"
              >
                <input type="file" (change)="onFile($event)" accept=".pdf,image/jpeg,image/png" class="hidden" />
                <span class="material-symbols-outlined text-primary" style="font-size:28px;">upload_file</span>
                <div>
                  @if (file) {
                    <p class="text-sm font-bold text-text-main">{{ file.name }}</p>
                    <p class="text-xs text-text-secondary">Haz click para cambiar</p>
                  } @else {
                    <p class="text-sm font-bold text-text-main">Seleccionar archivo</p>
                    <p class="text-xs text-text-secondary">o arrastra aquí</p>
                  }
                </div>
              </label>
            </label>

            @if (errorMessage()) {
              <div class="text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg p-3 flex items-start gap-2">
                <span class="material-symbols-outlined text-red-600 mt-0.5" style="font-size:18px;">error</span>
                <span>{{ errorMessage() }}</span>
              </div>
            }
            @if (success()) {
              <div class="text-sm text-emerald-700 bg-emerald-50 border border-emerald-200 rounded-lg p-3 flex items-start gap-2">
                <span class="material-symbols-outlined text-emerald-600 mt-0.5" style="font-size:18px;">check_circle</span>
                <span>Registro recibido. Te avisaremos por email cuando un administrador te apruebe.</span>
              </div>
            }

            <button
              type="submit"
              [disabled]="form.invalid || !file || loading()"
              class="flex w-full items-center justify-center rounded-lg h-12 px-4
                     bg-primary hover:bg-primary-hover transition-colors
                     text-white text-base font-bold tracking-[0.015em] shadow-sm
                     disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ loading() ? 'Enviando...' : 'Registrarme' }}
            </button>
          </form>

          <p class="text-sm text-text-secondary text-center">
            ¿Ya tienes cuenta?
            <a routerLink="/login" class="text-primary font-bold hover:underline">Inicia sesión</a>
          </p>
        </div>
      </div>
    </div>
  `,
})
export class RegisterComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  /**
   * Vinculado al query param `?role=...` gracias a withComponentInputBinding().
   * Si llega un valor válido, se selecciona ese tipo de cuenta por defecto.
   */
  @Input() role?: string;

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly success = signal(false);
  protected file: File | null = null;

  protected readonly roleOptions = [
    { value: 'COLLECTOR', label: 'Coleccionista' },
    { value: 'FOUNDATION', label: 'Museo / Fundación' },
    { value: 'TRANSPORT', label: 'Transporte' },
  ] as const;

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    phone: [''],
    taxId: [''],
    role: ['COLLECTOR' as RegisterRole, [Validators.required]],
  });

  ngOnInit(): void {
    if (this.role && this.isValidRole(this.role)) {
      this.form.controls.role.setValue(this.role);
    }
  }

  private isValidRole(value: string): value is RegisterRole {
    return value === 'COLLECTOR' || value === 'FOUNDATION' || value === 'TRANSPORT';
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.file = input.files?.[0] ?? null;
  }

  submit(): void {
    if (this.form.invalid || !this.file) return;
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
}
