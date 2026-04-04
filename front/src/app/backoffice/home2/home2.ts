import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService, AuthUser } from '../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-home2',
  standalone: false,
  templateUrl: './home2.html',
  styleUrls: ['./home2.css'],
})
export class Home2 implements OnInit {
  currentUser: AuthUser | null = null;
  showUserMenu = false;

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getLoggedUser();
  }

  toggleUserMenu(): void {
    this.showUserMenu = !this.showUserMenu;
  }

  closeUserMenu(): void {
    this.showUserMenu = false;
  }

  logout(): void {
    this.authService.logout();
    this.showUserMenu = false;
    this.router.navigate(['/auth/login']);
  }
}
