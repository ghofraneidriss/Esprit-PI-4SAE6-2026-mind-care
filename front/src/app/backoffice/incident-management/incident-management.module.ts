import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { IncidentListComponent } from './incident-list/incident-list.component';
import { IncidentTypesComponent } from './incident-types/incident-types.component';

const routes: Routes = [
    { path: 'list', component: IncidentListComponent, data: { mode: 'active' } },
    { path: 'history', component: IncidentListComponent, data: { mode: 'history' } },
    { path: 'types', component: IncidentTypesComponent },
    { path: '', redirectTo: 'list', pathMatch: 'full' }
];

@NgModule({
    imports: [
        CommonModule,
        ReactiveFormsModule,
        FormsModule,
        RouterModule.forChild(routes),
        IncidentListComponent,
        IncidentTypesComponent
    ]
})
export class IncidentManagementModule { }
