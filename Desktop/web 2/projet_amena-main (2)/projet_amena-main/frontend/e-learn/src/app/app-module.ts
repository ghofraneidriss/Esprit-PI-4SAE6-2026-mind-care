import { NgModule, provideBrowserGlobalErrorListeners } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { QRCodeComponent } from 'angularx-qrcode';

import { AppRoutingModule } from './app-routing-module';
import { App } from './app';
import { FeedbackManagementComponent } from './components/feedback-management/feedback-management.component';
import { ReclamationManagementComponent } from './components/reclamation-management/reclamation-management.component';

@NgModule({
  declarations: [
    App,
    FeedbackManagementComponent,
    ReclamationManagementComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FormsModule,
    QRCodeComponent,
    AppRoutingModule
  ],
  providers: [
    provideBrowserGlobalErrorListeners(),
  ],
  bootstrap: [App]
})
export class AppModule { }
