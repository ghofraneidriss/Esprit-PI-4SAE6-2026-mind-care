import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AssignmentService } from './assignment.service';

describe('AssignmentService', () => {
  let service: AssignmentService;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AssignmentService],
    }).compileComponents();

    service = TestBed.inject(AssignmentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should call the smart assignment endpoint', () => {
    service.smartAssign(21).subscribe();

    const req = httpMock.expectOne('http://localhost:8085/api/assignments/21/smart');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush({ id: 21 });
  });

  it('should call the manual assignment endpoint', () => {
    service.manualAssign(5, 44).subscribe();

    const req = httpMock.expectOne('http://localhost:8085/api/assignments/manual');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ missionId: 5, volunteerId: 44 });
    req.flush({ id: 1 });
  });
});
