import { Component, HostListener } from '@angular/core';

@Component({
  selector: 'app-frontoffice-header',
  standalone: false,
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {
  isAlzheimerMenuOpen = false;

  readonly alzheimerSections: Array<{ label: string; route?: string }> = [
    { label: 'Comprendre la maladie', route: '/alzheimer/comprendre-maladie' },
    { label: 'Decouverte de la maladie' },
    { label: 'Alzheimer en chiffres' },
    { label: 'Les stades' },
    { label: 'Les symptomes' },
    { label: 'Les causes' },
    { label: 'Le diagnostic' },
    { label: 'Les traitements' },
    { label: 'Alzheimer chez les jeunes' },
    { label: 'La forme hereditaire' },
    { label: 'La fin de vie' },
    { label: 'Glossaire' }
  ];

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
