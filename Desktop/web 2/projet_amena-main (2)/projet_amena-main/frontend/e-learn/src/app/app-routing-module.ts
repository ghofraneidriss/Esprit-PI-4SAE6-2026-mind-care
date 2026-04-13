import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FeedbackManagementComponent } from './components/feedback-management/feedback-management.component';
import { ReclamationManagementComponent } from './components/reclamation-management/reclamation-management.component';

const routes: Routes = [
  { path: '', redirectTo: 'feedbacks', pathMatch: 'full' },
  { path: 'feedbacks', component: FeedbackManagementComponent },
  { path: 'reclamations', component: ReclamationManagementComponent },
  { path: '**', redirectTo: 'feedbacks' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
