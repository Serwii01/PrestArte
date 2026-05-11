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
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
})
export class MainLayoutComponent {
  protected readonly auth = inject(AuthService);
  protected readonly year = new Date().getFullYear();

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

  /** Navegación contextual según rol. Todas las rutas viven bajo /app. */
  protected readonly navLinks = computed<NavLink[]>(() => {
    switch (this.auth.role()) {
      case 'ADMIN':
        return [{ label: 'Solicitudes', path: '/app/admin', icon: 'how_to_reg' }];
      case 'COLLECTOR':
        return [{ label: 'Mi colección', path: '/app/collector', icon: 'palette' }];
      case 'FOUNDATION':
        return [
          { label: 'Mi fundación', path: '/app/foundation', icon: 'account_balance' },
          { label: 'Catálogo', path: '/app/foundation/browse', icon: 'collections_bookmark' },
        ];
      case 'TRANSPORT':
        return [{ label: 'Servicios', path: '/app/transport', icon: 'local_shipping' }];
      default:
        return [];
    }
  });

  logout(): void {
    this.auth.logout();
  }
}
