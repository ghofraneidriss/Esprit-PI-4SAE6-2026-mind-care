import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BackofficeFooter } from './footer';

describe('BackofficeFooter', () => {
  let component: BackofficeFooter;
  let fixture: ComponentFixture<BackofficeFooter>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BackofficeFooter]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BackofficeFooter);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
