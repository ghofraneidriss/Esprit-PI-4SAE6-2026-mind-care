import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-item-stats',
  standalone: false,
  template: '',
})
export class ItemStatsComponent implements OnInit {
  constructor(private readonly router: Router) {}
  ngOnInit(): void {
    this.router.navigate(['/admin/lost-items'], { replaceUrl: true });
  }
}
