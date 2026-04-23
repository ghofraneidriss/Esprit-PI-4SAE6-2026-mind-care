import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { adminAccessGuard } from './guards/admin-access.guard';
import { frontofficeAccessGuard } from './guards/frontoffice-access.guard';

const routes: Routes = [
  {
    path: 'admin',
    canMatch: [adminAccessGuard],
    loadChildren: () =>
      import('./backoffice/backoffice-module').then((m) => m.BackofficeModule),
  },
  {
    path: '',
    canMatch: [frontofficeAccessGuard],
    loadChildren: () =>
      import('./frontoffice/frontoffice-module').then((m) => m.FrontofficeModule),
  },
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
