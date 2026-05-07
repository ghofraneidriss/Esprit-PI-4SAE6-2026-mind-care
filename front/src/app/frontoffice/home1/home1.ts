import { AfterViewInit, Component, NgZone } from '@angular/core';

declare global {
  interface Window {
    AOS?: { init: (options?: Record<string, unknown>) => void; refresh: () => void };
  }
}

@Component({
  selector: 'app-home1',
  standalone: false,
  templateUrl: './home1.html',
  styleUrls: ['./home1.css'],
})
export class Home1 implements AfterViewInit {
  constructor(private ngZone: NgZone) {}

  ngAfterViewInit(): void {
    this.ngZone.runOutsideAngular(() => {
      const run = () => {
        const aos = typeof window !== 'undefined' ? window.AOS : undefined;
        if (aos) {
          aos.init({
            duration: 900,
            easing: 'ease-out-cubic',
            once: true,
            offset: 72,
            delay: 0,
          });
        }
      };
      queueMicrotask(run);
      setTimeout(run, 120);
    });
  }
}
