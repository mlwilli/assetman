import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';

import { AppShellComponent } from './layout/app-shell/app-shell';
import { LoginPageComponent } from './pages/login/login';
import { DashboardPageComponent } from './pages/dashboard/dashboard';
import { ForbiddenPageComponent } from './pages/forbidden/forbidden';

import { AssetsPageComponent } from './pages/assets/assets';
import { AssetDetailPageComponent } from './pages/assets/asset-detail';
import { AssetFormPageComponent } from './pages/assets/asset-form';

const ASSET_MANAGE_ROLES = ['OWNER', 'ADMIN', 'MANAGER'] as const;

export const routes: Routes = [
  { path: 'login', component: LoginPageComponent },
  { path: 'forbidden', component: ForbiddenPageComponent },

  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', component: DashboardPageComponent },

      // Assets (list/detail: any authenticated role allowed by backend)
      { path: 'assets', component: AssetsPageComponent },
      { path: 'assets/:id', component: AssetDetailPageComponent },

      // Assets (create/edit: restricted)
      {
        path: 'assets/new',
        component: AssetFormPageComponent,
        canMatch: [roleGuard([...ASSET_MANAGE_ROLES])],
      },
      {
        path: 'assets/:id/edit',
        component: AssetFormPageComponent,
        canMatch: [roleGuard([...ASSET_MANAGE_ROLES])],
      },
    ],
  },

  { path: '**', redirectTo: '' },
];
