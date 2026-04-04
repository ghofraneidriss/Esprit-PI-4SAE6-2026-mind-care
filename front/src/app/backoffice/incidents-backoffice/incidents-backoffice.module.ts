import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IncidentsBackofficeRoutingModule } from './incidents-backoffice-routing.module';
import { IncidentsListComponent } from './incidents-list/incidents-list.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

@NgModule({
  declarations: [
    IncidentsListComponent
  ],
  imports: [
    CommonModule,
    IncidentsBackofficeRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule
  ]
})
export class IncidentsBackofficeModule { }
