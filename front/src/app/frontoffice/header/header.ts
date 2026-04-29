import { Component, OnInit } from '@angular/core';
import { AuthService, AuthUser } from '../auth/auth.service';

@Component({
  selector: 'app-header',
  standalone: false,
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header implements OnInit {
  isLoggedIn = false;
  user: AuthUser | null = null;

  constructor(private authService: AuthService) { }

  ngOnInit(): void {
    this.user = this.authService.getLoggedUser();
    this.isLoggedIn = !!this.user;
  }
}
