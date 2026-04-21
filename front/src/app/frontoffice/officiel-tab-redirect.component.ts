import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

/** Redirige /activities/metiers → /officiel/quiz-list?tab=photo (chemins CVP inchangés côté cible). */
@Component({
  standalone: true,
  template: '',
})
export class OfficielTabRedirectComponent {
  constructor() {
    const router = inject(Router);
    router.navigate(['/officiel/quiz-list'], {
      queryParams: { tab: 'photo' },
      replaceUrl: true,
    });
  }
}
