import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
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
import { ListPostRoutingModule } from './list-post-routing.module';
import { ListPostComponent } from './list-post.component';
import { SuccessAlertComponent } from './success-alert.component';
import { DeleteConfirmationModalComponent } from './delete-confirmation-modal.component';
import { SharedModule } from '../../../shared/shared.module';

@NgModule({
  declarations: [
    ListPostComponent,
    SuccessAlertComponent,
    DeleteConfirmationModalComponent
  ],
  imports: [
    CommonModule,
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
    ListPostRoutingModule,
    SharedModule
  ],
  exports: []
})
export class ListPostModule { }
