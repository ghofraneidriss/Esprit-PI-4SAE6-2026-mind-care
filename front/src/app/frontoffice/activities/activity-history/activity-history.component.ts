import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivityService, ActivityResult, Activity } from '../../../services/activity.service';

@Component({
    selector: 'app-activity-history',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './activity-history.component.html',
    styles: []
})
export class ActivityHistoryComponent implements OnInit {
    results: ActivityResult[] = [];
    activities: Map<number, string> = new Map();

    constructor(private activityService: ActivityService) { }

    ngOnInit(): void {
        // 1. Load attributes to get titles
        this.activityService.getAllActivities().subscribe(activities => {
            activities.forEach(a => this.activities.set(a.id!, a.title));

            // 2. Load results
            // TODO: Use actual logged-in patient ID
            this.activityService.getResultsByPatient(1).subscribe(data => {
                this.results = data;
            });
        });
    }

    getActivityTitle(id: number): string {
        return this.activities.get(id) || 'Unknown Activity';
    }

    formatDuration(seconds: number): string {
        const minutes = Math.floor(seconds / 60);
        const remainingSeconds = seconds % 60;
        return `${minutes}m ${remainingSeconds}s`;
    }
}
