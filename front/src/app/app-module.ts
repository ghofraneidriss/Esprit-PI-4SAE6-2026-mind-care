import { APP_INITIALIZER, NgModule, provideBrowserGlobalErrorListeners } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing-module';
import { App } from './app';

function initMindCareLightTheme(): () => void {
  return () => {
    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-bs-theme', 'light');
    }
  };
}

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { RouterModule } from '@angular/router';

@NgModule({
  declarations: [App],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    CommonModule,
    FormsModule,
    HttpClientModule,
    RouterModule
  ],
  providers: [
    provideBrowserGlobalErrorListeners(),
    { provide: APP_INITIALIZER, useFactory: initMindCareLightTheme, multi: true },
  ],
  bootstrap: [App],
})
export class AppModule { }
