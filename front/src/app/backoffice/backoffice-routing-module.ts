import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { adminGuard, localizationFeatureGuard, safeZoneManagerGuard } from '../core/guards/auth.guard';

import { Home2 } from './home2/home2';
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
import { AdminDashboard } from './forum-admin/admin-dashboard/admin-dashboard';
import { PostList } from './forum-admin/post-list/post-list';
import { CommentList } from './forum-admin/comment-list/comment-list';
import { ForumReports } from './forum-admin/forum-reports/forum-reports';

import { BackofficeLayoutComponent } from './backoffice-layout/backoffice-layout';
import { MoodTrackerComponent } from './mood-tracker/mood-tracker';

// Lazy load categories module
const categoriesRoutes = () => import('./forum-admin/categories/categories.module').then(m => m.CategoriesModule);

const routes: Routes = [
  {
    path: '',
    component: BackofficeLayoutComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('../officiel/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      { path: 'incidents-analytics', redirectTo: 'incidents-reported', pathMatch: 'full' },
      {
        path: 'quiz-management',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('../officiel/quiz-management/quiz-management.component').then(
            (m) => m.QuizManagementComponent
          ),
      },
      {
        path: 'photo-management',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('../officiel/photo-management/photo-management.component').then(
            (m) => m.PhotoManagementComponent
          ),
      },
      {
        path: 'results',
        loadComponent: () =>
          import('../officiel/results/results.component').then((m) => m.ResultsComponent),
      },
      {
        path: 'risk-analysis',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('../officiel/risk-analysis/risk-analysis.component').then(
            (m) => m.RiskAnalysisComponent
          ),
      },
      {
        path: 'performance',
        loadComponent: () =>
          import('../officiel/performance/performance.component').then(
            (m) => m.PerformanceComponent
          ),
      },
      {
        path: 'reports',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('../officiel/report/report.component').then((m) => m.ReportComponent),
      },
      {
        path: 'users',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('../officiel/user-management/user-management.component').then(
            (m) => m.UserManagementComponent
          ),
      },
      {
        path: 'critical-patients',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('../officiel/critical-patients/critical-patients.component').then(
            (m) => m.CriticalPatientsComponent
          ),
      },
      {
        path: 'safe-zones',
        canActivate: [safeZoneManagerGuard],
        loadComponent: () =>
          import('../officiel/localization-management/localization-management.component').then(
            (m) => m.LocalizationManagementComponent
          ),
      },
      {
        path: 'patient-movement',
        canActivate: [localizationFeatureGuard],
        loadComponent: () =>
          import('../officiel/movement-monitoring/movement-monitoring.component').then(
            (m) => m.MovementMonitoringComponent
          ),
      },
      { path: 'calendar', component: CalendarPage },
      { path: 'chat', component: ChatPage },
      { path: 'mood-tracker', component: MoodTrackerComponent },
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
      { path: 'forum', component: AdminDashboard },
      { path: 'forum/posts', component: PostList },
      { path: 'forum/ListPost', component: PostList },
      { path: 'forum/comments', component: CommentList, title: 'Forum Comments' },
      {
        path: 'moderation/forum-reports',
        component: ForumReports,
        title: 'Comment reports',
      },
      {
        path: 'forum/categories',
        canActivate: [adminGuard],
        loadChildren: categoriesRoutes,
        title: 'Forum Categories',
      },
      {
        path: 'incidents',
        loadChildren: () => import('./incident-management/incident-management.module').then(m => m.IncidentManagementModule),
        title: 'Incident Management'
      },
      {
        path: 'incidents-reported',
        loadChildren: () => import('./incidents-backoffice/incidents-backoffice.module').then(m => m.IncidentsBackofficeModule),
        title: 'Reported Incidents'
      },
      {
        path: 'incident-history',
        loadChildren: () => import('./incident-management/incident-management.module').then(m => m.IncidentManagementModule),
        title: 'Incident History'
      },
    ]
  },
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class BackofficeRoutingModule { }
