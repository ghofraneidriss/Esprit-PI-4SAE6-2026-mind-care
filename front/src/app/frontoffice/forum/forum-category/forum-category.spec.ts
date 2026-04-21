import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ForumCategory } from './forum-category';

describe('ForumCategory', () => {
  let component: ForumCategory;
  let fixture: ComponentFixture<ForumCategory>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ForumCategory]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ForumCategory);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
