import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

/**
 * Ruta '/app': componente vacío que redirige al dashboard del usuario según su rol.
 */
@Component({
  selector: 'app-home-redirect',
  standalone: true,
  template: ``,
})
export class HomeRedirectComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    switch (this.auth.role()) {
      case 'ADMIN':
        this.router.navigate(['/app/admin']);
        break;
      case 'COLLECTOR':
        this.router.navigate(['/app/collector']);
        break;
      case 'FOUNDATION':
        this.router.navigate(['/app/foundation']);
        break;
      case 'TRANSPORT':
        this.router.navigate(['/app/transport']);
        break;
      default:
        this.router.navigate(['/login']);
    }
  }
}
