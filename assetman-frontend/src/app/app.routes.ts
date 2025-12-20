import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';

const MANAGE_ROLES = ['OWNER', 'ADMIN', 'MANAGER'] as const;

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login').then(m => m.LoginPageComponent),
  },
  {
    path: 'forbidden',
    loadComponent: () =>
      import('./pages/forbidden/forbidden').then(m => m.ForbiddenPageComponent),
  },

  {
    path: '',
    loadComponent: () =>
      import('./layout/app-shell/app-shell').then(m => m.AppShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },

      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/dashboard/dashboard').then(m => m.DashboardPageComponent),
      },

      // Assets (list/detail)
      {
        path: 'assets',
        loadComponent: () =>
          import('./pages/assets/assets').then(m => m.AssetsPageComponent),
      },
      {
        path: 'assets/:id',
        loadComponent: () =>
          import('./pages/assets/asset-detail').then(m => m.AssetDetailPageComponent),
      },

      // Assets (create/edit: restricted)
      {
        path: 'assets/new',
        canMatch: [roleGuard([...MANAGE_ROLES])],
        loadComponent: () =>
          import('./pages/assets/asset-form').then(m => m.AssetFormPageComponent),
      },
      {
        path: 'assets/:id/edit',
        canMatch: [roleGuard([...MANAGE_ROLES])],
        loadComponent: () =>
          import('./pages/assets/asset-form').then(m => m.AssetFormPageComponent),
      },

      // Locations (list)
      {
        path: 'locations',
        loadComponent: () =>
          import('./pages/locations/locations').then(m => m.LocationsPageComponent),
      },

      // Locations (create/edit: restricted) - define BEFORE :id route
      {
        path: 'locations/new',
        canMatch: [roleGuard([...MANAGE_ROLES])],
        loadComponent: () =>
          import('./pages/locations/location-form').then(
            m => m.LocationFormPageComponent,
          ),
      },
      {
        path: 'locations/:id/edit',
        canMatch: [roleGuard([...MANAGE_ROLES])],
        loadComponent: () =>
          import('./pages/locations/location-form').then(
            m => m.LocationFormPageComponent,
          ),
      },

      // Locations (detail)
      {
        path: 'locations/:id',
        loadComponent: () =>
          import('./pages/locations/location-detail').then(
            m => m.LocationDetailPageComponent,
          ),
      },
    ],
  },

  { path: '**', redirectTo: '' },
];
