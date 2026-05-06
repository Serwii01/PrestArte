import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { Role } from '../../../core/models/user.models';

interface NavLink {
  label: string;
  path: string;
  icon: string;
}

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="flex min-h-screen w-full flex-col bg-background">
      <!-- Navbar Stitch -->
      <header class="sticky top-0 z-50 w-full bg-surface border-b border-border px-4 lg:px-10 py-3 shadow-sm">
        <div class="flex items-center justify-between gap-4 max-w-[1400px] mx-auto">
          <!-- Marca + nav -->
          <div class="flex items-center gap-8">
            <a routerLink="/" class="flex items-center gap-3">
              <div class="size-8 flex items-center justify-center bg-primary/10 rounded-lg text-primary">
                <span class="material-symbols-outlined">museum</span>
              </div>
              <h2 class="text-text-main text-xl font-bold leading-tight tracking-tight">Prestarte</h2>
            </a>

            <nav class="hidden lg:flex items-center gap-2 ml-4">
              @for (link of navLinks(); track link.path) {
                <a [routerLink]="link.path"
                   routerLinkActive="text-primary bg-primary/5"
                   [routerLinkActiveOptions]="{ exact: false }"
                   class="text-text-main text-sm font-semibold hover:text-primary
                          transition-colors px-3 py-1.5 rounded-lg flex items-center gap-2">
                  <span class="material-symbols-outlined" style="font-size:18px;">{{ link.icon }}</span>
                  {{ link.label }}
                </a>
              }
            </nav>
          </div>

          <!-- Búsqueda + acciones + avatar -->
          <div class="flex flex-1 justify-end gap-4 md:gap-6 items-center">
            <button type="button"
                    class="hidden sm:flex items-center justify-center size-10 rounded-lg
                           hover:bg-gray-soft transition-colors text-text-secondary">
              <span class="material-symbols-outlined">notifications</span>
            </button>

            <div class="flex items-center gap-3">
              <div class="size-10 rounded-full bg-primary/10 flex items-center justify-center
                          text-primary font-bold text-sm border border-border">
                {{ initial() }}
              </div>
              <div class="hidden md:block">
                <p class="text-sm font-bold text-text-main leading-tight">{{ auth.displayName() }}</p>
                <p class="text-xs text-text-secondary capitalize">{{ roleLabel() }}</p>
              </div>
              <button type="button" (click)="logout()"
                      title="Cerrar sesión"
                      class="size-9 flex items-center justify-center rounded-lg
                             hover:bg-red-50 hover:text-red-600 text-text-secondary transition-colors">
                <span class="material-symbols-outlined" style="font-size:20px;">logout</span>
              </button>
            </div>
          </div>
        </div>
      </header>

      <!-- Contenido -->
      <main class="flex-1 w-full max-w-[1400px] mx-auto px-4 lg:px-10 py-8">
        <router-outlet />
      </main>

      <footer class="text-center text-xs text-text-secondary py-6 border-t border-border bg-surface">
        Prestarte · Trabajo de Fin de Grado · {{ year }}
      </footer>
    </div>
  `,
})
export class MainLayoutComponent {
  protected readonly auth = inject(AuthService);
  protected readonly year = new Date().getFullYear();

  /** Inicial para el avatar circular del navbar. */
  protected readonly initial = computed(() => {
    const name = this.auth.displayName();
    return name ? name.charAt(0).toUpperCase() : '?';
  });

  protected readonly roleLabel = computed(() => {
    const labels: Record<Role, string> = {
      ADMIN: 'Administrador',
      COLLECTOR: 'Coleccionista',
      FOUNDATION: 'Fundación',
      TRANSPORT: 'Transporte',
    };
    const r = this.auth.role();
    return r ? labels[r] : '';
  });

  /** Navegación contextual según rol. */
  protected readonly navLinks = computed<NavLink[]>(() => {
    switch (this.auth.role()) {
      case 'ADMIN':
        return [
          { label: 'Solicitudes', path: '/admin', icon: 'how_to_reg' },
        ];
      case 'COLLECTOR':
        return [
          { label: 'Mi colección', path: '/collector', icon: 'palette' },
        ];
      case 'FOUNDATION':
        return [
          { label: 'Mi fundación', path: '/foundation', icon: 'account_balance' },
        ];
      case 'TRANSPORT':
        return [
          { label: 'Servicios', path: '/transport', icon: 'local_shipping' },
        ];
      default:
        return [];
    }
  });

  logout(): void {
    this.auth.logout();
  }
}
