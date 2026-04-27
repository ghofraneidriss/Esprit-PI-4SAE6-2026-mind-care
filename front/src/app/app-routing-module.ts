import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { adminGuard } from './core/guards/auth.guard';

const routes: Routes = [
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadChildren: () =>
      import('./backoffice/backoffice-module').then((m) => m.BackofficeModule),
  },
  {
    path: '',
    loadChildren: () =>
      import('./frontoffice/frontoffice-module').then((m) => m.FrontofficeModule),
  },
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, {
      scrollPositionRestoration: 'top',
    }),
  ],
  exports: [RouterModule],
})
export class AppRoutingModule { }
