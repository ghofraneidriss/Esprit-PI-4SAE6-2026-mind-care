import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DoctorPrescriptionService } from './doctor-prescription.service';
import { vi } from 'vitest';

describe('DoctorPrescriptionService', () => {
  let service: DoctorPrescriptionService;
  let httpMock: HttpTestingController;
  const apiUrl = 'http://localhost:8083/api/doctor';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DoctorPrescriptionService]
    });
    service = TestBed.inject(DoctorPrescriptionService);
    httpMock = TestBed.inject(HttpTestingController);

    // Initialiser localStorage mockup
    let store: any = {};
    const mockLocalStorage = {
      getItem: (key: string): string | null => {
        return key in store ? store[key] : null;
      },
      setItem: (key: string, value: string) => {
        store[key] = `${value}`;
      },
      removeItem: (key: string) => {
        delete store[key];
      },
      clear: () => {
        store = {};
      }
    };
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(mockLocalStorage.getItem);
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(mockLocalStorage.setItem);
    vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(mockLocalStorage.removeItem);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get history by patient id', () => {
    const dummyData = [{ id: 1, status: 'SIGNED' }];

    service.getHistoryByPatient(10).subscribe(res => {
      expect(res.length).toBe(1);
      expect(res).toEqual(dummyData);
    });

    const req = httpMock.expectOne(`${apiUrl}/prescriptions/patient/10`);
    expect(req.request.method).toBe('GET');
    req.flush(dummyData);
  });

  it('should check medicine overlap', () => {
    const alerts = [{ type: 'SAME_MEDICINE' }];

    service.checkMedicineOverlap(10, 5, '2023-10-01', '2023-10-10', 0).subscribe(res => {
      expect(res).toEqual(alerts);
    });

    const expectedUrl = `${apiUrl}/prescriptions/check-overlap?patientId=10&medicineId=5&startDate=2023-10-01&endDate=2023-10-10&currentPrescriptionId=0`;
    const req = httpMock.expectOne(expectedUrl);
    expect(req.request.method).toBe('GET');
    req.flush(alerts);
  });

  it('should check doctor shopping', () => {
    const alerts = [{ medicineName: 'Xanax' }];

    service.checkDoctorShopping(10, 5, 2).subscribe(res => {
      expect(res).toEqual(alerts);
    });

    const expectedUrl = `${apiUrl}/prescriptions/check-doctor-shopping?patientId=10&medicineId=5&currentDoctorId=2`;
    const req = httpMock.expectOne(expectedUrl);
    expect(req.request.method).toBe('GET');
    req.flush(alerts);
  });

  it('should save and retrieve draft from local storage', () => {
    const draft = { patientId: 1 };
    service.saveDraft(draft);
    const retrievedDraft = service.getDraft();

    expect(retrievedDraft).toEqual(draft);
    expect(Storage.prototype.setItem).toHaveBeenCalled();
    expect(Storage.prototype.getItem).toHaveBeenCalled();
  });
});
