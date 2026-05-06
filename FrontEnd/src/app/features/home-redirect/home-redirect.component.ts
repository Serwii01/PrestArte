import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

/**
 * Ruta '/' protegida: redirige al dashboard según el rol del usuario.
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
    const role = this.auth.role();
    switch (role) {
      case 'ADMIN':
        this.router.navigate(['/admin']);
        break;
      case 'COLLECTOR':
        this.router.navigate(['/collector']);
        break;
      case 'FOUNDATION':
        this.router.navigate(['/foundation']);
        break;
      case 'TRANSPORT':
        this.router.navigate(['/transport']);
        break;
      default:
        this.router.navigate(['/login']);
    }
  }
}
