import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PrescriptionService } from './prescription.service';

describe('PrescriptionService (Frontoffice)', () => {
  let service: PrescriptionService;
  let httpMock: HttpTestingController;
  const apiUrl = 'http://localhost:8083/api/patient/prescriptions';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PrescriptionService]
    });
    service = TestBed.inject(PrescriptionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get history by patient id', () => {
    const dummyPrescriptions = [
      { id: 1, status: 'SIGNED' },
      { id: 2, status: 'COMPLETED' }
    ];

    service.getMyHistory(10).subscribe(res => {
      expect(res.length).toBe(2);
      expect(res).toEqual(dummyPrescriptions);
    });

    const req = httpMock.expectOne(`${apiUrl}/my-history/10`);
    expect(req.request.method).toBe('GET');
    req.flush(dummyPrescriptions);
  });

  it('should get prescription by id', () => {
    const dummyPrescription = { id: 1, status: 'SIGNED' };

    service.getPrescriptionById(1).subscribe(res => {
      expect(res).toEqual(dummyPrescription);
    });

    const req = httpMock.expectOne(`${apiUrl}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(dummyPrescription);
  });
});
