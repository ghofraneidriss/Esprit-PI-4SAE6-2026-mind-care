import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AdminMedicineService } from './admin-medicine.service';

describe('AdminMedicineService', () => {
  let service: AdminMedicineService;
  let httpMock: HttpTestingController;
  const apiUrl = 'http://localhost:8083/api/admin/medicines';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AdminMedicineService]
    });
    service = TestBed.inject(AdminMedicineService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get all medicines', () => {
    const dummyMedicines = [{ id: 1, commercialName: 'Doliprane' }];

    service.getAll().subscribe(res => {
      expect(res.length).toBe(1);
      expect(res).toEqual(dummyMedicines);
    });

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('GET');
    req.flush(dummyMedicines);
  });

  it('should delete a medicine', () => {
    service.delete(1).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });

  it('should import excel file', () => {
    const file = new File(['dummy content'], 'medicines.xlsx', { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });

    service.importExcel(file).subscribe(res => {
      expect(res).toEqual('Imported successfully');
    });

    const req = httpMock.expectOne(`${apiUrl}/import`);
    expect(req.request.method).toBe('POST');
    req.flush('Imported successfully');
  });
});
