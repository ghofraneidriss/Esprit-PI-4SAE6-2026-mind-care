import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { IncidentsBackofficeRoutingModule } from './incidents-backoffice-routing.module';
import { IncidentsListComponent } from './incidents-list/incidents-list.component';
import { DashboardHomeComponent } from '../dashboard-home/dashboard-home';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

@NgModule({
  declarations: [
    IncidentsListComponent
  ],
  imports: [
    CommonModule,
    RouterModule,
    DashboardHomeComponent,
    IncidentsBackofficeRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule
  ]
})
export class IncidentsBackofficeModule { }
