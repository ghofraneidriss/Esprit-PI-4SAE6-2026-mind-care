import { Component, OnInit, OnDestroy, Renderer2 } from '@angular/core';

@Component({
    selector: 'app-frontoffice-layout',
    templateUrl: './frontoffice-layout.html',
    standalone: false
})
export class FrontofficeLayoutComponent implements OnInit, OnDestroy {
    constructor(private renderer: Renderer2) { }

    ngOnInit(): void {
        // Set light theme and frontoffice class
        this.renderer.addClass(document.body, 'frontoffice-theme');
        this.renderer.setAttribute(document.documentElement, 'data-bs-theme', 'light');
        // Ensure the background is white/light
        this.renderer.setStyle(document.body, 'background-color', '#ffffff');
    }

    ngOnDestroy(): void {
        // Clean up
        this.renderer.removeClass(document.body, 'frontoffice-theme');
        this.renderer.removeStyle(document.body, 'background-color');
    }
}
