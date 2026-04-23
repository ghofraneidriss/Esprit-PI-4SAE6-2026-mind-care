import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ActivityService, Activity } from '../../services/activity.service';

@Component({
  selector: 'app-activities-front',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './activities.component.html',
  styleUrls: ['./activities.component.css']
})
export class ActivitiesFrontPage implements OnInit {
  activities: Activity[] = [];
  filteredActivities: Activity[] = [];
  selectedType: string = 'ALL';

  viewMode: 'TYPES' | 'THEMES' | 'ACTIVITIES' = 'TYPES';
  selectedCategory: string = '';
  availableCategories: string[] = [];

  constructor(
    private activityService: ActivityService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.activityService.getAllActivities().subscribe(data => {
      console.log('Fetched activities:', data);
      this.activities = data;
      this.filteredActivities = data;
    }, error => {
      console.error('Error fetching activities:', error);
    });
  }

  selectType(type: string) {
    this.selectedType = type;
    console.log('Selected type:', type);

    // Si c'est le type QUIZ, rediriger vers la page des quiz
    if (type === 'QUIZ') {
      this.router.navigate(['/quiz']);
      return;
    }

    if (type === 'ALL') {
      this.filteredActivities = this.activities;
      this.viewMode = 'ACTIVITIES';
      return;
    }

    // Filter activities by type to find available categories
    const typeActivities = this.activities.filter(a => String(a.type).toUpperCase() === type);

    // Extract unique categories
    const categories = new Set(typeActivities.map(a => a.category).filter(c => c !== undefined && c !== null));
    this.availableCategories = Array.from(categories) as string[];

    console.log('Available categories for ' + type + ':', this.availableCategories);

    if (this.availableCategories.length === 0) {
      // If no categories, go straight to activities
      this.filteredActivities = typeActivities;
      this.viewMode = 'ACTIVITIES';
    } else {
      this.viewMode = 'THEMES';
    }
  }

  selectCategory(category: string) {
    this.selectedCategory = category;
    console.log('Selected category:', category);

    this.filteredActivities = this.activities.filter(a =>
      String(a.type).toUpperCase() === this.selectedType &&
      a.category === category
    );

    this.viewMode = 'ACTIVITIES';
  }

  goBack() {
    if (this.viewMode === 'ACTIVITIES' && this.selectedType !== 'ALL' && this.availableCategories.length > 0) {
      this.viewMode = 'THEMES';
    } else {
      this.viewMode = 'TYPES';
      this.selectedType = 'ALL';
    }
  }

  // kept for backward compatibility if needed, but selectType is preferred
  filterActivities(type: string) {
    this.selectType(type);
  }

  startActivity(id: number): void {
    this.router.navigate(['/activities/execute', id]);
  }

  goToHistory(): void {
    this.router.navigate(['/activities/history']);
  }
}