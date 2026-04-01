import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { strictAdminGuard } from '../guards/strict-admin.guard';

import { Home2 } from './home2/home2';
import { BackofficeShellComponent } from './backoffice-shell/backoffice-shell';

import { UserManagementPage } from './user-management/user-management';
import { MedicalReportsPageComponent } from './medical-reports-page/medical-reports-page';
import { FilesManagementPageComponent } from './files-management/files-management';
import { VolunteeringPageComponent } from './volunteering/volunteering';
import { AssignmentHistoryPageComponent } from './assignment-history/assignment-history';

const routes: Routes = [
  {
    path: '',
    component: BackofficeShellComponent,
    children: [
      { path: '', component: Home2, pathMatch: 'full' },
      { path: 'user-management', component: UserManagementPage, canActivate: [strictAdminGuard] },
      { path: 'medical-reports', component: MedicalReportsPageComponent },
      { path: 'files-management', component: FilesManagementPageComponent },
      { path: 'volunteering', component: VolunteeringPageComponent },
      { path: 'assignment-history', component: AssignmentHistoryPageComponent },
      { path: '**', redirectTo: '' },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class BackofficeRoutingModule { }
