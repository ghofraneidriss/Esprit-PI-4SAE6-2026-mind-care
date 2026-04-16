import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ForumLayout } from './forum-layout';

describe('ForumLayout', () => {
  let component: ForumLayout;
  let fixture: ComponentFixture<ForumLayout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ForumLayout]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ForumLayout);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
