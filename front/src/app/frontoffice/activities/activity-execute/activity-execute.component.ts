import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ActivityService, Activity, ActivityResult } from '../../../services/activity.service';

@Component({
  selector: 'app-activity-execute',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './activity-execute.component.html',
  styleUrls: ['./activity-execute.component.css']
})
export class ActivityExecuteComponent implements OnInit {
  activity: Activity | null = null;
  currentContentIndex: number = 0;
  score: number = 0;
  startTime: number = 0;

  constructor(
    private route: ActivatedRoute,
    private activityService: ActivityService,
    private router: Router
  ) { }

  get currentContent() {
    return this.activity?.contents ? this.activity.contents[this.currentContentIndex] : null;
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.startTime = Date.now();
    this.activityService.getActivityById(id).subscribe(data => {
      this.activity = data;
    });
  }

  getProgress(): number {
    if (!this.activity?.contents?.length) return 0;
    return ((this.currentContentIndex) / this.activity.contents.length) * 100;
  }

  selectAnswer(answer: string): void {
    if (this.currentContent && answer === this.currentContent.correctAnswer) {
      this.score++;
    }
    this.currentContentIndex++;

    if (this.currentContentIndex >= (this.activity?.contents?.length || 0)) {
      this.submitResult();
    }
  }

  submitResult(): void {
    if (!this.activity) return;

    const duration = Math.floor((Date.now() - this.startTime) / 1000);
    const result: ActivityResult = {
      patientId: 1, // TODO: Get logged in patient ID
      activityId: this.activity.id!,
      score: this.score,
      duration: duration
    };

    this.activityService.submitResult(result).subscribe(() => {
      console.log('Result submitted');
    });
  }

  goBack(): void {
    this.router.navigate(['/activities']);
  }
}