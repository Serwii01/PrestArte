import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { Role } from '../models/user.models';

/**
 * Restringe rutas a uno o varios roles. Uso:
 *
 *   canActivate: [authGuard, roleGuard(['ADMIN'])]
 */
export const roleGuard = (allowed: Role[]): CanActivateFn => {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    const role = auth.role();
    if (role && allowed.includes(role)) return true;

    // Si está logueado pero el rol no aplica, lo mandamos al área autenticada
    // (HomeRedirect lo dispara a su dashboard real).
    router.navigate(['/app']);
    return false;
  };
};
