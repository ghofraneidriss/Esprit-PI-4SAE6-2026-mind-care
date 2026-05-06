import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterModule } from '@angular/router';
import { of } from 'rxjs';

import { BackofficeHeader } from './header';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { NgZoneUiSync } from '../../core/services/ng-zone-ui-sync.service';

describe('BackofficeHeader', () => {
  let component: BackofficeHeader;
  let fixture: ComponentFixture<BackofficeHeader>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BackofficeHeader, RouterModule.forRoot([])],
      providers: [
        {
          provide: AuthService,
          useValue: { getUserId: () => null, getCurrentUser: () => null, logout: () => {} }
        },
        {
          provide: NotificationService,
          useValue: {
            startPolling: () => {},
            stopPolling: () => {},
            unreadCount: of(0),
            getByUser: () => of([]),
            refreshUnreadCount: () => {},
            markAllRead: () => of(null),
            delete: () => of(null),
            deleteAllForUser: () => of(null),
            getUnread: () => of([]),
            markRead: () => of(null)
          }
        },
        {
          provide: NgZoneUiSync,
          useValue: { apply: (_cdr: unknown, fn: () => void) => fn() }
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BackofficeHeader);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
