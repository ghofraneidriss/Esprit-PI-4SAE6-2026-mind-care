import { Component, OnInit } from '@angular/core';
import { ActivityService, Activity, ActivityType, ActivityLevel } from '../../services/activity.service';

@Component({
  selector: 'app-activities',
  standalone: false,
  templateUrl: './activities.html',
  styleUrls: ['./activities.css'],
})
export class ActivitiesPage implements OnInit {
  activities: Activity[] = [];
  newActivity: Activity = {
    title: '',
    description: '',
    type: 'QUIZ',
    level: 'EASY'
  };

  constructor(private activityService: ActivityService) { }

  ngOnInit(): void {
    this.loadActivities();
  }

  loadActivities() {
    this.activityService.getAllActivities().subscribe(data => {
      this.activities = data;
    });
  }

  createActivity() {
    this.activityService.createActivity(this.newActivity).subscribe(() => {
      this.loadActivities();
      // Close modal logic if needed (handled by bootstrap data-bs-dismiss usually)
      this.newActivity = { title: '', description: '', type: 'QUIZ', level: 'EASY' }; // Reset
    });
  }

  // Content Management
  selectedActivity: Activity | null = null;
  newContent: any = {
    question: '',
    optionA: '',
    optionB: '',
    optionC: '',
    correctAnswer: '',
    imageUrl: '' // Optional for Image Recognition
  };

  openManageContent(activity: Activity) {
    this.selectedActivity = activity;
    this.newContent = { question: '', optionA: '', optionB: '', optionC: '', correctAnswer: '', imageUrl: '' };
  }

  addContent() {
    if (this.selectedActivity && this.selectedActivity.id) {
      this.activityService.addContent(this.selectedActivity.id, this.newContent).subscribe(() => {
        // Refresh activity to show new content
        this.activityService.getActivityById(this.selectedActivity!.id!).subscribe(updatedActivity => {
          this.selectedActivity = updatedActivity;
          // Update the list as well
          const index = this.activities.findIndex(a => a.id === updatedActivity.id);
          if (index !== -1) {
            this.activities[index] = updatedActivity;
          }
          this.newContent = { question: '', optionA: '', optionB: '', optionC: '', correctAnswer: '', imageUrl: '' };
        });
      });
    }
  }

  deleteActivity(id: number) {
    if (confirm('Are you sure you want to delete this activity?')) {
      this.activityService.deleteActivity(id).subscribe(() => {
        this.loadActivities();
      });
    }
  }
}
