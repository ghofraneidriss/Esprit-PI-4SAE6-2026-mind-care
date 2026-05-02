import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { of } from 'rxjs';

import { AuthService } from '../../frontoffice/auth/auth.service';
import { VolunteerService } from './volunteer.service';

describe('VolunteerService', () => {
  let service: VolunteerService;
  let httpMock: HttpTestingController;

  const authServiceStub = {
    getLoggedUser: () => ({ userId: 11, firstName: 'Amina', lastName: 'Said' }),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        VolunteerService,
        { provide: AuthService, useValue: authServiceStub },
      ],
    }).compileComponents();

    service = TestBed.inject(VolunteerService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load the volunteer directory', () => {
    const mockDirectory = [
      {
        userId: 1,
        initials: 'AS',
        name: 'Amina Said',
        verified: true,
        city: 'Tunis',
        availability: 'Available now',
        skills: ['Patient Care'],
        rating: 4.8,
        missions: 5,
        avatarColor: '#3b82f6',
      },
    ];

    service.getVolunteerDirectory().subscribe((directory) => {
      expect(directory).toEqual(mockDirectory);
    });

    const req = httpMock.expectOne('http://localhost:8085/api/volunteers/directory');
    expect(req.request.method).toBe('GET');
    req.flush(mockDirectory);
  });

  it('should post the volunteer presence session payload', () => {
    service.markOnline(42, '  Lina  ').subscribe();

    const req = httpMock.expectOne('http://localhost:8085/api/volunteers/42/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.displayName).toBe('Lina');
    expect(req.request.body.userId).toBe(42);
    expect(req.request.body.sessionId).toContain('volunteer-42-');
    req.flush(null);
  });

  it('should fall back to the legacy assignment endpoint when assign-and-notify is missing', () => {
    const expectedPayload = {
      mission: { id: 7 },
      volunteerId: 13,
      volunteerUserId: 13,
      notes: 'Handle this soon',
    };

    service.createAssignment(7, 13, 'Handle this soon').subscribe((result) => {
      expect(result).toEqual({ id: 9 });
    });

    const first = httpMock.expectOne('http://localhost:8085/api/volunteers/assign-and-notify');
    expect(first.request.method).toBe('POST');
    expect(first.request.body).toEqual({
      missionId: 7,
      volunteerId: 13,
      notes: 'Handle this soon',
    });
    first.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    const fallback = httpMock.expectOne('http://localhost:8085/api/volunteer/assignments');
    expect(fallback.request.method).toBe('POST');
    expect(fallback.request.body).toEqual(expectedPayload);
    fallback.flush({ id: 9 });
  });
});
