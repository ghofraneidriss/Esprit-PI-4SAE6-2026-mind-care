import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { strictAdminGuard } from '../guards/strict-admin.guard';

import { Home2 } from './home2/home2';
import { ActivitiesPage } from './activities/activities';
import { CalendarPage } from './calendar/calendar';
import { ChatPage } from './chat/chat';
import { CustomersPage } from './customers/customers';
import { DealsPage } from './deals/deals';
import { EmployeePage } from './employee/employee';
import { FinancePage } from './finance/finance';
import { ProfilePage } from './profile/profile';
import { ReviewPage } from './review/review';
import { SalesPage } from './sales/sales';
import { SettingsPage } from './settings/settings';
import { TaskManagementPage } from './task-management/task-management';
import { TeamManagementPage } from './team-management/team-management';
import { UserManagementPage } from './user-management/user-management';
import { MedicalReportsPageComponent } from './medical-reports-page/medical-reports-page';
import { FilesManagementPageComponent } from './files-management/files-management';
import { VolunteeringPageComponent } from './volunteering/volunteering';
import { AssignmentHistoryPageComponent } from './assignment-history/assignment-history';

const routes: Routes = [
  { path: '', component: Home2 },
  { path: 'user-management', component: UserManagementPage, canActivate: [strictAdminGuard] },
  { path: 'medical-reports', component: MedicalReportsPageComponent },
  { path: 'files-management', component: FilesManagementPageComponent },
  { path: 'volunteering', component: VolunteeringPageComponent },
  { path: 'assignment-history', component: AssignmentHistoryPageComponent },
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class BackofficeRoutingModule { }
