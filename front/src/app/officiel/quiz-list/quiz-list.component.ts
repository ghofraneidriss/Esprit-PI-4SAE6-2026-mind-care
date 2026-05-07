import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { QuizService } from '../../core/services/quiz.service';
import { PhotoService } from '../../core/services/photo.service';
import { QuizLimitService, QuizLimitStatus } from '../../core/services/quiz-limit.service';
import { AuthService } from '../../core/services/auth.service';
import { resolveActivitiesMediaUrl } from '../../../environments/environment';

@Component({
  selector: 'app-off-quiz-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './quiz-list.component.html',
  styleUrls: ['./quiz-list.component.css'],
})
export class QuizListComponent implements OnInit {
  quizzes: any[] = [];
  photos: any[] = [];
  loading = true;
  activeTab: 'quiz' | 'photo' = 'quiz';

  readonly resolveActivitiesMediaUrl = resolveActivitiesMediaUrl;

  limitStatus: QuizLimitStatus | null = null;

  constructor(
    private quizSvc: QuizService,
    private photoSvc: PhotoService,
    private cdr: ChangeDetectorRef,
    private quizLimitSvc: QuizLimitService,
    private auth: AuthService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const qp = this.route.snapshot.queryParamMap.get('tab');
    if (qp === 'photo') {
      this.activeTab = 'photo';
    } else if (qp === 'quiz') {
      this.activeTab = 'quiz';
    }
    this.route.queryParamMap.subscribe((q) => {
      const t = q.get('tab');
      if (t === 'photo') {
        this.activeTab = 'photo';
      } else if (t === 'quiz') {
        this.activeTab = 'quiz';
      }
      this.cdr.detectChanges();
    });
    this.loadAll();
    this.loadLimitStatus();
  }

  loadAll(): void {
    this.loading = true;
    this.loaded = 0;
    this.quizSvc.getQuizzes().subscribe({
      next: (d: any) => {
        this.quizzes = d || [];
        this.checkDone();
      },
      error: () => {
        this.quizzes = [];
        this.checkDone();
      },
    });
    this.photoSvc.getPhotos().subscribe({
      next: (d) => {
        this.photos = d || [];
        this.checkDone();
      },
      error: () => {
        this.photos = [];
        this.checkDone();
      },
    });
  }

  loadLimitStatus(): void {
    const uid = this.auth.getUserId();
    if (uid != null && this.auth.getRole() === 'PATIENT') {
      this.quizLimitSvc.getStatus(uid).subscribe({
        next: (s) => {
          this.limitStatus = s;
          this.cdr.detectChanges();
        },
        error: () => {},
      });
    }
  }

  private loaded = 0;
  checkDone(): void {
    if (++this.loaded >= 2) {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  getDiffLabel(d: string): string {
    return { EASY: 'Easy', MEDIUM: 'Medium', HARD: 'Hard' }[d] || d;
  }

  get isLimitReached(): boolean {
    return !!this.limitStatus && this.limitStatus.hasLimit && !this.limitStatus.canPlay;
  }
}
