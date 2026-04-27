import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import {
  ForumService,
  Post,
  ForumStatsPayload,
} from '../../../core/services/forum.service';
import { AuthService } from '../../../core/services/auth.service';
import { NgZoneUiSync } from '../../../core/services/ng-zone-ui-sync.service';

/**
 * CVP / front office : indicateurs forum + top catégories + historique des fils archivés (inactifs).
 * (Non exposé dans le back-office admin.)
 */
@Component({
  selector: 'app-forum-cvp-insights',
  templateUrl: './forum-cvp-insights.component.html',
  styleUrls: [
    '../forum-shared.css',
    '../forum-home/forum-home.component.css',
    './forum-cvp-insights.component.css',
  ],
  standalone: false,
})
export class ForumCvpInsightsComponent implements OnInit {
  stats: ForumStatsPayload | null = null;
  /** True when /forum/stats failed but /forum/home provided thread totals only. */
  statsPartial = false;
  inactiveRows: Post[] = [];
  loading = true;
  error: string | null = null;

  constructor(
    private forumService: ForumService,
    private authService: AuthService,
    private readonly cdr: ChangeDetectorRef,
    private readonly zoneUi: NgZoneUiSync
  ) {}

  ngOnInit(): void {
    this.zoneUi.scheduleInitialLoad(() => this.loadForumInsights());
  }

  private loadForumInsights(): void {
    const uid = this.authService.getUserId();
    this.loading = true;
    const inactive$ = this.forumService.getInactivePostsHistory(uid ?? undefined, 200).pipe(
      catchError(() => of([] as Post[]))
    );

    forkJoin({
      stats: this.forumService.getForumStats().pipe(catchError(() => of(null))),
      inactive: inactive$,
    })
      .pipe(
        switchMap(({ stats, inactive }) => {
          if (stats != null) {
            return of({ stats, inactive, partial: false });
          }
          return this.forumService
            .getForumHome({
              postSort: 'recent',
              postPage: 0,
              postSize: 1,
            })
            .pipe(
              map((home) => {
                const fallback: ForumStatsPayload = {
                  totalPosts: home.totalThreadCount ?? 0,
                  totalComments: 0,
                  inactivePosts: 0,
                  topCategories: [],
                };
                return { stats: fallback, inactive, partial: true };
              }),
              catchError(() => of({ stats: null as ForumStatsPayload | null, inactive, partial: false }))
            );
        })
      )
      .subscribe({
        next: ({ stats, inactive, partial }) => {
          this.zoneUi.apply(this.cdr, () => {
            this.stats = stats;
            this.statsPartial = partial;
            this.inactiveRows = inactive ?? [];
            if (!stats) {
              this.error =
                'Unable to load forum statistics. Ensure forums-service is running (port 8082) and try again.';
            } else {
              this.error = null;
            }
            this.loading = false;
          });
        },
        error: () => {
          this.zoneUi.apply(this.cdr, () => {
            this.error = 'Unable to load forum insights.';
            this.loading = false;
          });
        },
      });
  }

  trackById(_i: number, p: Post): number {
    return p.id;
  }
}
