import { Component, Inject, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  standalone: false,
  styleUrls: ['./app.css']
})
export class App {
  protected readonly title = signal('MindCare');

  constructor(
    private readonly router: Router,
    @Inject(DOCUMENT) private readonly document: Document,
  ) {
    this.applyLightTheme();
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => {
        const preloader = document.getElementById('preloader');
        if (preloader) {
          preloader.remove();
        }
        this.applyLightTheme();
      });
  }

  private applyLightTheme(): void {
    const html = this.document.documentElement;
    html.setAttribute('data-bs-theme', 'light');
    this.document.body.style.backgroundColor = '#ffffff';
  }
}
