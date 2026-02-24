import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

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

const routes: Routes = [
  { path: '', component: Home2 },
  { path: 'reports', component: Home2 },
  { path: 'activities', component: ActivitiesPage },
  { path: 'calendar', component: CalendarPage },
  { path: 'chat', component: ChatPage },
  { path: 'customers', component: CustomersPage },
  { path: 'deals', component: DealsPage },
  { path: 'employee', component: EmployeePage },
  { path: 'finance', component: FinancePage },
  { path: 'profile', component: ProfilePage },
  { path: 'review', component: ReviewPage },
  { path: 'sales', component: SalesPage },
  { path: 'settings', component: SettingsPage },
  { path: 'task-management', component: TaskManagementPage },
  { path: 'team-management', component: TeamManagementPage },
  { path: 'user-management', component: UserManagementPage },
  { path: 'medical-reports', component: MedicalReportsPageComponent },
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class BackofficeRoutingModule {}
