import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/services/auth.service';

/**
 * Mi cuenta: derechos sobre los datos personales (RGPD). Descargar todos
 * los datos en JSON y darse de baja.
 */
@Component({
  selector: 'app-account',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <header class="mb-6">
      <h1 class="text-3xl md:text-4xl font-black text-text-main tracking-tight">Mi cuenta</h1>
      <p class="text-text-secondary mt-1">{{ auth.displayName() }}</p>
    </header>

    @if (message()) {
      <div class="mb-4 text-sm text-emerald-700 bg-emerald-50 border border-emerald-200 rounded-lg p-3">{{ message() }}</div>
    }
    @if (error()) {
      <div class="mb-4 text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg p-3">{{ error() }}</div>
    }

    <div class="space-y-6 max-w-2xl">
      <section class="stitch-card p-6">
        <h2 class="text-lg font-bold text-text-main mb-1 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">download</span> Descargar mis datos
        </h2>
        <p class="text-sm text-text-secondary mb-4">
          Un fichero JSON con tu perfil, tus obras, tus préstamos, tus avisos y tus valoraciones
          (derecho de acceso y portabilidad).
        </p>
        <button type="button" (click)="exportData()" [disabled]="busy()"
                class="inline-flex items-center gap-2 rounded-lg h-10 px-4 bg-primary hover:bg-primary-hover text-white text-sm font-bold disabled:opacity-50">
          Descargar
        </button>
      </section>

      <section class="stitch-card p-6">
        <h2 class="text-lg font-bold text-text-main mb-1 flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">policy</span> Textos legales
        </h2>
        <ul class="text-sm space-y-1">
          <li><a routerLink="/legal/privacidad" class="text-primary hover:underline">Política de privacidad</a></li>
          <li><a routerLink="/legal/terminos" class="text-primary hover:underline">Términos de uso</a></li>
          <li><a routerLink="/legal/cookies" class="text-primary hover:underline">Política de cookies</a></li>
        </ul>
      </section>

      <section class="stitch-card p-6 border-red-200">
        <h2 class="text-lg font-bold text-red-700 mb-1 flex items-center gap-2">
          <span class="material-symbols-outlined">person_remove</span> Darme de baja
        </h2>
        <p class="text-sm text-text-secondary mb-4">
          Anonimizaremos tus datos personales y eliminaremos tu documento de verificación. Tus obras dejarán de
          estar en el catálogo. Los préstamos ya terminados se conservan durante los plazos legales porque
          también son datos de las otras partes. No se puede deshacer y no es posible con préstamos en curso.
        </p>
        @if (!confirming()) {
          <button type="button" (click)="confirming.set(true)"
                  class="inline-flex items-center gap-2 rounded-lg h-10 px-4 border border-red-300 text-red-600 hover:bg-red-50 text-sm font-bold">
            Quiero darme de baja
          </button>
        } @else {
          <label class="block max-w-sm">
            <p class="text-sm font-medium text-text-main pb-1">Confirma con tu contraseña</p>
            <input type="password" name="password" [(ngModel)]="password" autocomplete="current-password"
                   class="w-full rounded-lg border border-border h-11 px-3 text-sm" />
          </label>
          <div class="mt-3 flex gap-2">
            <button type="button" (click)="confirming.set(false)"
                    class="px-4 py-2 rounded-lg bg-white border border-border text-sm font-medium">Cancelar</button>
            <button type="button" (click)="deleteAccount()" [disabled]="!password || busy()"
                    class="px-4 py-2 rounded-lg bg-red-600 hover:bg-red-700 text-white text-sm font-bold disabled:opacity-50">
              Darme de baja definitivamente
            </button>
          </div>
        }
      </section>
    </div>
  `,
})
export class AccountComponent {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly busy = signal(false);
  protected readonly confirming = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected password = '';

  exportData(): void {
    this.busy.set(true);
    this.error.set(null);
    this.http.get(`${environment.apiBaseUrl}/me/export`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'mis-datos-prestarte.json';
        a.click();
        URL.revokeObjectURL(url);
        this.busy.set(false);
        this.message.set('Datos descargados.');
      },
      error: () => {
        this.busy.set(false);
        this.error.set('No se pudieron descargar tus datos.');
      },
    });
  }

  deleteAccount(): void {
    this.busy.set(true);
    this.error.set(null);
    this.http.post(`${environment.apiBaseUrl}/me/delete`, { password: this.password }).subscribe({
      next: () => {
        this.auth.logout();
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo completar la baja.');
      },
    });
  }
}
