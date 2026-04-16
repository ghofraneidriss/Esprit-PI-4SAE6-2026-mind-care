import { Component, HostListener } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-frontoffice-header, app-header',
  standalone: false,
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {
  isAlzheimerMenuOpen = false;

  readonly alzheimerSections: Array<{ label: string; route?: string }> = [
    { label: 'Comprendre la maladie', route: '/alzheimer/comprendre-maladie' },
    { label: 'Découverte de la maladie', route: '/alzheimer/decouverte' },
    { label: 'Alzheimer en chiffres', route: '/alzheimer/chiffres' },
    { label: 'Les stades', route: '/alzheimer/stades' },
    { label: 'Les symptômes', route: '/alzheimer/symptomes' },
    { label: 'Les causes', route: '/alzheimer/causes' },
    { label: 'Le diagnostic', route: '/alzheimer/diagnostic' },
    { label: 'Les traitements', route: '/alzheimer/traitements' },
    { label: 'Alzheimer chez les jeunes', route: '/alzheimer/jeunes' },
    { label: 'La forme héréditaire', route: '/alzheimer/hereditaire' },
    { label: 'La fin de vie', route: '/alzheimer/fin-de-vie' },
    { label: 'Glossaire', route: '/alzheimer/glossaire' }
  ];

  constructor(private readonly router: Router) {}

  get isLoggedIn(): boolean {
    return !!localStorage.getItem('loggedUser');
  }

  logout(): void {
    localStorage.removeItem('loggedUser');
    this.router.navigate(['/auth/signup']);
  }

  toggleAlzheimerMenu(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.isAlzheimerMenuOpen = !this.isAlzheimerMenuOpen;
  }

  keepMenuOpen(event: Event): void {
    event.stopPropagation();
  }

  closeAlzheimerMenu(): void {
    this.isAlzheimerMenuOpen = false;
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.closeAlzheimerMenu();
  }
}
