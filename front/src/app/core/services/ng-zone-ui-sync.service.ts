import { Injectable, NgZone, ChangeDetectorRef } from '@angular/core';

/**
 * Synchronise les mises à jour RxJS/HTTP avec la zone Angular et la détection des changements.
 * Sans cela, certains écrans (lazy route, layout CVP) restent en spinner jusqu’à un clic utilisateur.
 */
@Injectable({ providedIn: 'root' })
export class NgZoneUiSync {
  constructor(private readonly ngZone: NgZone) {}

  /** Exécute la mutation d’état puis force un cycle de détection (à utiliser depuis les callbacks `subscribe`). */
  apply(cdr: ChangeDetectorRef, fn: () => void): void {
    this.ngZone.run(() => {
      fn();
      cdr.detectChanges();
    });
  }

  /** Reporte le chargement initial au macrotask suivant (après router / layout). */
  scheduleInitialLoad(loadFn: () => void): void {
    setTimeout(() => this.ngZone.run(() => loadFn()), 0);
  }
}
