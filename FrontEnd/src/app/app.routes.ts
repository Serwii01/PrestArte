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
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password.component').then(
        (m) => m.ForgotPasswordComponent,
      ),
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password.component').then(
        (m) => m.ResetPasswordComponent,
      ),
  },
  // ===== Catálogo público (sin sesión) =====
  {
    path: 'catalog',
    loadComponent: () =>
      import('./features/catalog/public-catalog.component').then(
        (m) => m.PublicCatalogComponent,
      ),
  },
  {
    path: 'catalog/:id',
    loadComponent: () =>
      import('./features/artwork-detail/artwork-detail.component').then(
        (m) => m.ArtworkDetailComponent,
      ),
  },
  {
    path: 'partners',
    loadComponent: () =>
      import('./features/partners/public-partners.component').then(
        (m) => m.PublicPartnersComponent,
      ),
  },
  {
    path: 'partners/:id',
    loadComponent: () =>
      import('./features/partners/partner-detail.component').then(
        (m) => m.PartnerDetailComponent,
      ),
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
        path: 'artworks/:id',
        loadComponent: () =>
          import('./features/artwork-detail/artwork-detail.component').then(
            (m) => m.ArtworkDetailComponent,
          ),
      },
      {
        path: 'loans/:id',
        loadComponent: () =>
          import('./features/loan-detail/loan-detail.component').then(
            (m) => m.LoanDetailComponent,
          ),
      },
      {
        path: 'loans/:id/chat',
        loadComponent: () =>
          import('./features/chat/chat.component').then((m) => m.ChatComponent),
      },
      {
        path: 'collector',
        canActivate: [roleGuard(['COLLECTOR'])],
        children: [
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () =>
              import('./features/collector/collector-dashboard/collector-dashboard.component').then(
                (m) => m.CollectorDashboardComponent,
              ),
          },
          {
            path: 'upload',
            loadComponent: () =>
              import('./features/collector/upload-artwork/upload-artwork.component').then(
                (m) => m.UploadArtworkComponent,
              ),
          },
          {
            path: 'edit/:editId',
            loadComponent: () =>
              import('./features/collector/upload-artwork/upload-artwork.component').then(
                (m) => m.UploadArtworkComponent,
              ),
          },
        ],
      },
      {
        path: 'foundation',
        canActivate: [roleGuard(['FOUNDATION'])],
        children: [
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () =>
              import('./features/foundation/foundation-dashboard/foundation-dashboard.component').then(
                (m) => m.FoundationDashboardComponent,
              ),
          },
          {
            path: 'browse',
            loadComponent: () =>
              import('./features/foundation/browse-artworks/browse-artworks.component').then(
                (m) => m.BrowseArtworksComponent,
              ),
          },
          {
            path: 'request-loan/:artworkId',
            loadComponent: () =>
              import('./features/foundation/request-loan/request-loan.component').then(
                (m) => m.RequestLoanComponent,
              ),
          },
        ],
      },
      {
        path: 'transport',
        canActivate: [roleGuard(['TRANSPORT'])],
        children: [
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () =>
              import('./features/transport/transport-dashboard/transport-dashboard.component').then(
                (m) => m.TransportDashboardComponent,
              ),
          },
          {
            path: 'profile',
            loadComponent: () =>
              import('./features/transport/profile/transport-profile.component').then(
                (m) => m.TransportProfileComponent,
              ),
          },
        ],
      },
    ],
  },

  // Cualquier otra ruta → landing
  { path: '**', redirectTo: '' },
];
