import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  // ===== Rutas públicas =====
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./features/landing/landing.component').then((m) => m.LandingComponent),
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },

  // ===== Rutas autenticadas (bajo /app) =====
  {
    path: 'app',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./shared/components/main-layout/main-layout.component').then(
        (m) => m.MainLayoutComponent,
      ),
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () =>
          import('./features/home-redirect/home-redirect.component').then(
            (m) => m.HomeRedirectComponent,
          ),
      },
      {
        path: 'admin',
        canActivate: [roleGuard(['ADMIN'])],
        loadComponent: () =>
          import('./features/admin/admin-dashboard/admin-dashboard.component').then(
            (m) => m.AdminDashboardComponent,
          ),
      },
      {
        path: 'collector',
        canActivate: [roleGuard(['COLLECTOR'])],
        loadComponent: () =>
          import('./features/collector/collector-dashboard/collector-dashboard.component').then(
            (m) => m.CollectorDashboardComponent,
          ),
      },
      {
        path: 'foundation',
        canActivate: [roleGuard(['FOUNDATION'])],
        loadComponent: () =>
          import('./features/foundation/foundation-dashboard/foundation-dashboard.component').then(
            (m) => m.FoundationDashboardComponent,
          ),
      },
      {
        path: 'transport',
        canActivate: [roleGuard(['TRANSPORT'])],
        loadComponent: () =>
          import('./features/transport/transport-dashboard/transport-dashboard.component').then(
            (m) => m.TransportDashboardComponent,
          ),
      },
    ],
  },

  // Cualquier otra ruta → landing
  { path: '**', redirectTo: '' },
];
