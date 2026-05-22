import { CommonModule } from '@angular/common';
import { Component, computed, ElementRef, HostListener, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
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
  protected readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);
  protected readonly year = new Date().getFullYear();

  /** Estado del dropdown de notificaciones. */
  protected readonly notifOpen = signal(false);

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
          // El catálogo es público y único: la única diferencia para una
          // fundación con sesión activa es que en la ficha aparece el botón
          // "Solicitar préstamo".
          { label: 'Catálogo', path: '/catalog', icon: 'collections_bookmark' },
        ];
      case 'TRANSPORT':
        return [{ label: 'Servicios', path: '/app/transport', icon: 'local_shipping' }];
      default:
        return [];
    }
  });

  toggleNotifications(): void {
    if (!this.notifOpen()) this.notifications.refresh();
    this.notifOpen.update((v) => !v);
  }

  closeNotifications(): void {
    this.notifOpen.set(false);
  }

  /** Click en una notificación: navega al destino y cierra el dropdown. */
  openNotification(link: any[]): void {
    this.notifOpen.set(false);
    this.router.navigate(link);
  }

  /** Cierra el dropdown si se hace click fuera del navbar. */
  @HostListener('document:click', ['$event'])
  onDocClick(event: MouseEvent): void {
    if (!this.notifOpen()) return;
    const target = event.target as Node;
    if (!this.host.nativeElement.contains(target)) {
      this.notifOpen.set(false);
    }
  }

  logout(): void {
    this.auth.logout();
  }
}
