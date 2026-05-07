import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { IncidentListComponent } from './incident-list/incident-list.component';

const routes: Routes = [
    { path: 'list', component: IncidentListComponent, data: { mode: 'active' } },
    { path: 'history', component: IncidentListComponent, data: { mode: 'history' } },
    { path: '', redirectTo: 'list', pathMatch: 'full' }
];

@NgModule({
    imports: [
        CommonModule,
        ReactiveFormsModule,
        FormsModule,
        RouterModule.forChild(routes),
        IncidentListComponent
    ]
})
export class IncidentManagementModule { }
