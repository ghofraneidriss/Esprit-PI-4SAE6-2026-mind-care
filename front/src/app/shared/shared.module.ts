import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { Header } from '../frontoffice/header/header';
import { Footer } from '../frontoffice/footer/footer';
import { RouterModule } from '@angular/router';

@NgModule({
  declarations: [
    Header,
    Footer
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule
  ],
  exports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    Header,
    Footer,
    RouterModule
  ]
})
export class SharedModule { }
