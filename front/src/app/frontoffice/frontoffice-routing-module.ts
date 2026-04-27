import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {
  adminGuard,
  authGuard,
  localizationFeatureGuard,
  redirectStaffFromOfficielGuard,
  redirectStaffFromPublicHomeGuard,
  safeZoneManagerGuard,
} from '../core/guards/auth.guard';

import { Home1 } from './home1/home1';
import { FrontofficeLayoutComponent } from './frontoffice-layout';
import { LoginCoverAuthPage } from './auth/login-cover/login-cover';
import { RegisterCoverAuthPage } from './auth/register-cover/register-cover';
import { ForgotPasswordCoverAuthPage } from './auth/forgot-password-cover/forgot-password-cover';
import { NewPasswordCoverAuthPage } from './auth/new-password-cover/new-password-cover';
import { FrontofficeActivitiesPage } from './activities/activities';
import { AlzheimerUnderstandingFrontPage } from './alzheimer-understanding/alzheimer-understanding';
import { RecommendationsPage } from './recommendations/recommendations';
import { SouvenirsFrontPage } from './souvenirs/souvenirs';
import { PuzzlePlayPage } from './puzzle-play/puzzle-play';
import { SudokuPlayPage } from './sudoku-play/sudoku-play';

/**
 * CVP / activity routes: same as the source project (officiel/quiz-list, officiel/play/...).
 * Screen content is rendered in the MindCare layout (header / main / footer).
 */
const routes: Routes = [
  { path: 'login-cover', component: LoginCoverAuthPage },
  { path: 'register-cover', component: RegisterCoverAuthPage },
  { path: 'forgot-password-cover', component: ForgotPasswordCoverAuthPage },
  { path: 'new-password-cover', component: NewPasswordCoverAuthPage },
  {
    path: '',
    component: FrontofficeLayoutComponent,
    children: [
      {
        path: '',
        component: Home1,
        pathMatch: 'full',
        canActivate: [redirectStaffFromPublicHomeGuard],
      },
      {
        path: 'forum',
        loadChildren: () => import('./forum/forum.module').then((m) => m.ForumModule),
      },
      {
        path: 'profile',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./user-profile/user-profile.component').then((m) => m.UserProfileComponent),
      },
      {
        path: 'incidents',
        canActivate: [authGuard],
        loadChildren: () => import('./incidents-front.module').then((m) => m.IncidentsFrontModule),
      },
      {
        path: 'patient-movement',
        canActivate: [authGuard, localizationFeatureGuard],
        loadComponent: () =>
          import('../officiel/movement-monitoring/movement-monitoring.component').then(
            (m) => m.MovementMonitoringComponent
          ),
      },
      {
        path: 'safe-zones',
        canActivate: [authGuard, safeZoneManagerGuard],
        loadComponent: () =>
          import('../officiel/localization-management/localization-management.component').then(
            (m) => m.LocalizationManagementComponent
          ),
      },
      {
        path: 'community-activities',
        component: FrontofficeActivitiesPage,
      },
      {
        path: 'recommendations',
        component: RecommendationsPage,
        canActivate: [authGuard],
      },
      {
        path: 'souvenirs',
        component: SouvenirsFrontPage,
        canActivate: [authGuard],
      },
      {
        path: 'puzzle-play/:eventId',
        component: PuzzlePlayPage,
        canActivate: [authGuard],
      },
      {
        path: 'puzzles/:eventId',
        component: PuzzlePlayPage,
        canActivate: [authGuard],
      },
      {
        path: 'sudoku-play/:eventId',
        component: SudokuPlayPage,
        canActivate: [authGuard],
      },
      {
        path: 'alzheimer/comprendre-maladie',
        component: AlzheimerUnderstandingFrontPage,
      },
      {
        path: 'alzheimer/decouverte',
        component: AlzheimerUnderstandingFrontPage,
      },
      {
        path: 'alzheimer/chiffres',
        component: AlzheimerUnderstandingFrontPage,
      },
      {
        path: 'alzheimer/stades',
        component: AlzheimerUnderstandingFrontPage,
      },
      {
        path: 'alzheimer/symptomes',
        component: AlzheimerUnderstandingFrontPage,
      },
      {
        path: 'alzheimer/causes',
        component: AlzheimerUnderstandingFrontPage,
      },
      {
        path: 'alzheimer/diagnostic',
        component: AlzheimerUnderstandingFrontPage,
      },
      {
        path: 'alzheimer/traitements',
        component: AlzheimerUnderstandingFrontPage,
      },
      {
        path: 'alzheimer/jeunes',
        component: AlzheimerUnderstandingFrontPage,
      },
      {
        path: 'alzheimer/hereditaire',
        component: AlzheimerUnderstandingFrontPage,
      },
      {
        path: 'alzheimer/fin-de-vie',
        component: AlzheimerUnderstandingFrontPage,
      },
      {
        path: 'alzheimer/glossaire',
        component: AlzheimerUnderstandingFrontPage,
      },

      // --- Aliases → officiel (CVP) paths ---
      { path: 'activities', redirectTo: 'officiel/quiz-list', pathMatch: 'full' },
      { path: 'activities/quiz', redirectTo: 'officiel/quiz-list', pathMatch: 'full' },
      {
        path: 'activities/metiers',
        canActivate: [redirectStaffFromOfficielGuard],
        loadComponent: () =>
          import('./officiel-tab-redirect.component').then((m) => m.OfficielTabRedirectComponent),
      },
      {
        path: 'activities/play/:type/:id',
        canActivate: [redirectStaffFromOfficielGuard],
        loadComponent: () =>
          import('../officiel/quiz-player/quiz-player.component').then((m) => m.QuizPlayerComponent),
      },

      // --- Officiel / CVP (same URLs as the original project) ---
      {
        path: 'officiel/play/:type/:id',
        canActivate: [redirectStaffFromOfficielGuard],
        loadComponent: () =>
          import('../officiel/quiz-player/quiz-player.component').then((m) => m.QuizPlayerComponent),
      },
      {
        path: 'officiel/login',
        canActivate: [redirectStaffFromOfficielGuard],
        loadComponent: () =>
          import('../officiel/auth/login/login.component').then((m) => m.OfficielLoginComponent),
      },
      {
        path: 'officiel/register',
        canActivate: [redirectStaffFromOfficielGuard],
        loadComponent: () =>
          import('../officiel/auth/register/register.component').then((m) => m.OfficielRegisterComponent),
      },
      {
        path: 'officiel/quiz-management',
        canActivate: [redirectStaffFromOfficielGuard, adminGuard],
        loadComponent: () =>
          import('../officiel/quiz-management/quiz-management.component').then(
            (m) => m.QuizManagementComponent
          ),
      },
      {
        path: 'officiel/photo-management',
        canActivate: [redirectStaffFromOfficielGuard, adminGuard],
        loadComponent: () =>
          import('../officiel/photo-management/photo-management.component').then(
            (m) => m.PhotoManagementComponent
          ),
      },
      {
        path: 'officiel/risk-analysis',
        canActivate: [redirectStaffFromOfficielGuard, adminGuard],
        loadComponent: () =>
          import('../officiel/risk-analysis/risk-analysis.component').then(
            (m) => m.RiskAnalysisComponent
          ),
      },
      {
        path: 'officiel/reports',
        canActivate: [redirectStaffFromOfficielGuard, adminGuard],
        loadComponent: () =>
          import('../officiel/report/report.component').then((m) => m.ReportComponent),
      },
      {
        path: 'officiel/users',
        canActivate: [redirectStaffFromOfficielGuard, adminGuard],
        loadComponent: () =>
          import('../officiel/user-management/user-management.component').then(
            (m) => m.UserManagementComponent
          ),
      },
      /** CVP patient : sidebar commune (dashboard, quiz-list, results, performance) */
      {
        path: 'officiel',
        canActivate: [redirectStaffFromOfficielGuard],
        loadComponent: () =>
          import('../officiel/officiel-activities-shell/officiel-activities-shell.component').then(
            (m) => m.OfficielActivitiesShellComponent
          ),
        children: [
          { path: '', pathMatch: 'full', redirectTo: 'quiz-list' },
          {
            path: 'dashboard',
            canActivate: [authGuard],
            loadComponent: () =>
              import('../officiel/dashboard/dashboard.component').then((m) => m.DashboardComponent),
          },
          {
            path: 'quiz-list',
            loadComponent: () =>
              import('../officiel/quiz-list/quiz-list.component').then((m) => m.QuizListComponent),
          },
          {
            path: 'results',
            loadComponent: () =>
              import('../officiel/results/results.component').then((m) => m.ResultsComponent),
          },
          {
            path: 'performance',
            loadComponent: () =>
              import('../officiel/performance/performance.component').then(
                (m) => m.PerformanceComponent
              ),
          },
        ],
      },

      // Legacy template routes → home
      { path: 'about', redirectTo: '', pathMatch: 'full' },
      { path: 'appointment', redirectTo: '', pathMatch: 'full' },
      { path: 'contact', redirectTo: '', pathMatch: 'full' },
      { path: 'department-details', redirectTo: '', pathMatch: 'full' },
      { path: 'departments', redirectTo: '', pathMatch: 'full' },
      { path: 'doctors', redirectTo: '', pathMatch: 'full' },
      { path: 'faq', redirectTo: '', pathMatch: 'full' },
      { path: 'gallery', redirectTo: '', pathMatch: 'full' },
      { path: 'privacy', redirectTo: '', pathMatch: 'full' },
      { path: 'service-details', redirectTo: '', pathMatch: 'full' },
      { path: 'services', redirectTo: '', pathMatch: 'full' },
      { path: 'starter-page', redirectTo: '', pathMatch: 'full' },
      { path: 'terms', redirectTo: '', pathMatch: 'full' },
      { path: 'testimonials', redirectTo: '', pathMatch: 'full' },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class FrontofficeRoutingModule {}
