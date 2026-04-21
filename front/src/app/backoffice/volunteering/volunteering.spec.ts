import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of } from 'rxjs';

import { AuthService } from '../../frontoffice/auth/auth.service';
import { VolunteerService } from './volunteer.service';
import { VolunteeringPageComponent } from './volunteering';

describe('VolunteeringPageComponent', () => {
  let component: VolunteeringPageComponent;
  let fixture: ComponentFixture<VolunteeringPageComponent>;
  let volunteerService: {
    getAll: ReturnType<typeof vi.fn>;
    getVolunteerDirectory: ReturnType<typeof vi.fn>;
    getPresenceStatus: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
    createAssignment: ReturnType<typeof vi.fn>;
  };

  const authServiceStub = {
    isAdmin: () => true,
    getLoggedUser: () => ({ userId: 11, firstName: 'Amina', lastName: 'Said' }),
  };

  beforeEach(async () => {
    volunteerService = {
      getAll: vi.fn().mockReturnValue(of([])),
      getVolunteerDirectory: vi.fn().mockReturnValue(of([])),
      getPresenceStatus: vi.fn().mockReturnValue(of([])),
      create: vi.fn().mockImplementation((payload: Record<string, unknown>) => of({
        id: 1,
        ...payload,
      })),
      delete: vi.fn().mockReturnValue(of(void 0)),
      createAssignment: vi.fn().mockReturnValue(of({ id: 2 })),
    };

    await TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule, HttpClientTestingModule],
      declarations: [VolunteeringPageComponent],
      providers: [
        { provide: AuthService, useValue: authServiceStub },
        { provide: VolunteerService, useValue: volunteerService },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(VolunteeringPageComponent);
    component = fixture.componentInstance;
  });

  it('should debounce location autocomplete lookups', async () => {
    vi.useFakeTimers();
    const searchSpy = vi.spyOn(component, 'searchLocations').mockImplementation(() => undefined as never);

    component.onLocationInput('Tun');
    await vi.advanceTimersByTimeAsync(299);
    expect(searchSpy).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(1);
    expect(searchSpy).toHaveBeenCalledWith('Tun');
    vi.useRealTimers();
  });

  it('should select a location suggestion and clear the suggestion list', () => {
    component.locationSuggestions = [
      { displayName: 'Tunis, Tunisia', lat: '0', lon: '0' },
    ];

    component.chooseLocationSuggestion({ displayName: 'Tunis, Tunisia', lat: '0', lon: '0' });

    expect(component.newMission.location).toBe('Tunis, Tunisia');
    expect(component.selectedLocationSuggestion?.displayName).toBe('Tunis, Tunisia');
    expect(component.locationSuggestions).toEqual([]);
  });

  it('should create a mission with the selected autocomplete location', () => {
    component.openCreateModal();
    component.patients = [
      { userId: 33, firstName: 'Sara', lastName: 'Ben Ali' } as any,
    ];
    component.newMission.title = 'Home Visit';
    component.newMission.category = 'Home Visit';
    component.newMission.duration = '2 hours';
    component.newMission.patientId = 33;

    component.chooseLocationSuggestion({
      displayName: 'Rue Habib Bourguiba, Tunis',
      lat: '36.8',
      lon: '10.18',
    });

    component.saveMission();

    expect(volunteerService.create).toHaveBeenCalledWith({
      title: 'Home Visit',
      category: 'Home Visit',
      location: 'Rue Habib Bourguiba, Tunis',
      duration: '2 hours',
      assignee: 'Unassigned',
      description: 'Patient: Sara Ben Ali',
      priority: 'MEDIUM',
      status: 'OPEN',
    });
    expect(component.missions.length).toBe(1);
    expect(component.missions[0].location).toBe('Rue Habib Bourguiba, Tunis');
    expect(component.missions[0].status).toBe('Open');
    expect(component.missions[0].priority).toBe('Medium');
    expect(component.isCreateModalOpen).toBe(false);
  });
});
