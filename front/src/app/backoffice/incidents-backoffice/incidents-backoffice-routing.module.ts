import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { IncidentsListComponent } from './incidents-list/incidents-list.component';

const routes: Routes = [
  {
    path: '',
    component: IncidentsListComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class IncidentsBackofficeRoutingModule { }
