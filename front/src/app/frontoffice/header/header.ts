import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-header',
  standalone: false,
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {

  constructor(private readonly router: Router) { }

  get isLoggedIn(): boolean {
    return !!localStorage.getItem('loggedUser');
  }

  logout(): void {
    localStorage.removeItem('loggedUser');
    this.router.navigate(['/auth/signup']);
  }
}
