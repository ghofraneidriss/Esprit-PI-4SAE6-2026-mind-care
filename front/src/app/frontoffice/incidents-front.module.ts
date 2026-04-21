import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { incidentReportGuard } from '../core/guards/auth.guard';
import { IncidentReportFrontPage } from './incident-report/incident-report-front';
import { PatientIncidentsComponent } from './incident-list-front/patient-incidents';

const routes: Routes = [
    { path: 'report', component: IncidentReportFrontPage, canActivate: [incidentReportGuard] },
    { path: 'history', component: PatientIncidentsComponent },
    { path: '', redirectTo: 'history', pathMatch: 'full' }
];

@NgModule({
    imports: [
        CommonModule,
        ReactiveFormsModule,
        FormsModule,
        RouterModule.forChild(routes),
        IncidentReportFrontPage,
        PatientIncidentsComponent
    ]
})
export class IncidentsFrontModule { }
