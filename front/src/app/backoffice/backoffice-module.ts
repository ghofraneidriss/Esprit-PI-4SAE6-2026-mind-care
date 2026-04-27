import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';

import { BackofficeRoutingModule } from './backoffice-routing-module';
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
import { BackofficeHeader } from './header/header';
import { BackofficeFooter } from './footer/footer';
import { SharedModule } from '../shared/shared.module';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommentList } from './forum-admin/comment-list/comment-list';
import { ForumReports } from './forum-admin/forum-reports/forum-reports';
import { ConfirmDialog } from './forum-admin/confirm-dialog/confirm-dialog';
import { BackofficeLayoutComponent } from './backoffice-layout/backoffice-layout';
import { DashboardHomeComponent } from './dashboard-home/dashboard-home';
import { AdminDashboard } from './forum-admin/admin-dashboard/admin-dashboard';
import { PostList } from './forum-admin/post-list/post-list';
import { DeletePostModal } from './forum-admin/delete-post-modal/delete-post-modal';
import { ViewPostModal } from './forum-admin/view-post-modal/view-post-modal';
import { PostForm } from './forum-admin/post-form/post-form';
import { MoodTrackerComponent } from './mood-tracker/mood-tracker';
import { MedicalEventsPage } from './medical-events/medical-events';
import { RecommendationPage } from './recommendation/recommendation';
import { SouvenirsPage } from './souvenirs/souvenirs';

@NgModule({
  declarations: [
    Home2,
    CalendarPage,
    ChatPage,
    CustomersPage,
    DealsPage,
    EmployeePage,
    FinancePage,
    ProfilePage,
    ReviewPage,
    SalesPage,
    SettingsPage,
    TaskManagementPage,
    TeamManagementPage,
    UserManagementPage,
    CommentList,
    ForumReports,
    ConfirmDialog,
    AdminDashboard,
    PostList,
    DeletePostModal,
    ViewPostModal,
    PostForm,
    MoodTrackerComponent,
    MedicalEventsPage,
    RecommendationPage,
    SouvenirsPage
  ],
  imports: [
    CommonModule,
    DashboardHomeComponent,
    RouterModule,
    BackofficeRoutingModule,
    ReactiveFormsModule,
    FormsModule,
    MatDialogModule,
    MatSnackBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    SharedModule,
    BackofficeHeader,
    BackofficeFooter,
    BackofficeLayoutComponent
  ],
})
export class BackofficeModule { }
