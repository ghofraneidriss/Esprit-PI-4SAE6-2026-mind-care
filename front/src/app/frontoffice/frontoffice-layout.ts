import { AfterViewInit, Component, Inject, OnInit } from '@angular/core';
import { DOCUMENT } from '@angular/common';

@Component({
    selector: 'app-frontoffice-layout',
    template: `
    <div class="alzcare-shell d-flex flex-column min-vh-100">
      <app-header></app-header>
      <main class="alzcare-main flex-grow-1">
        <router-outlet></router-outlet>
      </main>
      <app-footer></app-footer>
      <app-global-overlays />
    </div>
  `,
    styles: [
        `
      :host {
        display: block;
      }
    `,
    ],
    standalone: false,
})
export class FrontofficeLayoutComponent implements OnInit, AfterViewInit {
    constructor(@Inject(DOCUMENT) private document: Document) {}

    ngOnInit(): void {
        this.applyLightTheme();
    }

    ngAfterViewInit(): void {
        // After jQuery in backoffice main.js applies cookie / OS dark theme
        setTimeout(() => this.applyLightTheme(), 0);
    }

    private applyLightTheme(): void {
        const html = this.document.documentElement;
        html.setAttribute('data-bs-theme', 'light');
        html.classList.remove('backoffice-body-theme');
        this.document.body.style.backgroundColor = '#ffffff';
    }
}
