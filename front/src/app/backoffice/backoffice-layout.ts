import { Component, OnInit, OnDestroy, Renderer2 } from '@angular/core';

@Component({
    selector: 'app-backoffice-layout',
    templateUrl: './backoffice-layout.html',
    standalone: false
})
export class BackofficeLayoutComponent implements OnInit, OnDestroy {
    constructor(private renderer: Renderer2) { }

    ngOnInit(): void {
        // Force dark theme and backoffice class
        this.renderer.addClass(document.body, 'backoffice-theme');
        this.renderer.setAttribute(document.documentElement, 'data-bs-theme', 'dark');
        // Ensure the background is dark
        this.renderer.setStyle(document.body, 'background-color', '#1E1E1E');
    }

    ngOnDestroy(): void {
        // Reset background and remove class
        this.renderer.removeClass(document.body, 'backoffice-theme');
        this.renderer.removeStyle(document.body, 'background-color');
        this.renderer.setAttribute(document.documentElement, 'data-bs-theme', 'light');
    }
}
