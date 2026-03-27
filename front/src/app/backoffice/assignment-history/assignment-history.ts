import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../frontoffice/auth/auth.service';
import { VolunteerService } from '../volunteering/volunteer.service';
import { Mission, MissionPriority } from '../volunteering/volunteering';

export interface AssignmentHistory {
    id: number;
    missionIcon: string;
    missionTitle: string;
    volunteerName: string;
    category: string;
    patientName: string;
    patientInitials: string;
    patientColor: string;
    assignedDate: string;
    missionDate: string;
    duration: string;
    rating?: number;
    status: 'Assigned' | 'In Progress' | 'Cancelled' | 'Completed';
    statusColor: 'blue' | 'yellow' | 'red' | 'green';
}

@Component({
    selector: 'app-assignment-history-page',
    standalone: false,
    templateUrl: './assignment-history.html',
    styleUrls: ['./assignment-history.css'],
})
export class AssignmentHistoryPageComponent implements OnInit {
    searchQuery = '';
    activeFilter = 'All';
    showVerifiedOnly = false;

    // Stats
    totalAssignments = 156;
    activeAssignments = 23;
    completedMissions = 118;
    averageRating = 4.8;

    assignments: AssignmentHistory[] = [
        {
            id: 1,
            missionIcon: 'fi fi-rr-home',
            missionTitle: 'Weekly Companion...',
            volunteerName: 'James Rodriguez',
            category: 'Home Visit',
            patientName: 'Frank O\'S...',
            patientInitials: 'F',
            patientColor: '#3b82f6',
            assignedDate: 'Mar 28, 2026',
            missionDate: '2026-04-08',
            duration: '2 hours',
            status: 'Assigned',
            statusColor: 'blue',
        },
        {
            id: 2,
            missionIcon: 'fi fi-rr-home',
            missionTitle: 'Evening Meal Prepa...',
            volunteerName: 'Robert Martinez',
            category: 'Home Visit',
            patientName: 'Dorothy Si...',
            patientInitials: 'D',
            patientColor: '#3b82f6',
            assignedDate: 'Mar 25, 2026',
            missionDate: '2026-04-05',
            duration: '3 hours',
            status: 'Assigned',
            statusColor: 'blue',
        },
        {
            id: 3,
            missionIcon: 'fi fi-rr-home',
            missionTitle: 'Respite Care - Even...',
            volunteerName: 'Patricia Williams',
            category: 'Home Visit',
            patientName: 'William An...',
            patientInitials: 'W',
            patientColor: '#3b82f6',
            assignedDate: 'Mar 20, 2026',
            missionDate: '2026-04-02',
            duration: '4 hours',
            status: 'Assigned',
            statusColor: 'blue',
        },
        {
            id: 4,
            missionIcon: 'fi fi-rr-car-side',
            missionTitle: 'Transportation to M...',
            volunteerName: 'Michael Torres',
            category: 'Transport',
            patientName: 'Robert Ch...',
            patientInitials: 'R',
            patientColor: '#3b82f6',
            assignedDate: 'Mar 19, 2026',
            missionDate: '2026-03-30',
            duration: '3 hours',
            status: 'In Progress',
            statusColor: 'yellow',
        },
        {
            id: 5,
            missionIcon: 'fi fi-rr-shopping-bag',
            missionTitle: 'Grocery Shopping ...',
            volunteerName: 'Emily Chen',
            category: 'Errands',
            patientName: 'Patricia W...',
            patientInitials: 'P',
            patientColor: '#3b82f6',
            assignedDate: 'Jan 2, 2026',
            missionDate: '2026-01-05',
            duration: '2 hours',
            status: 'Cancelled',
            statusColor: 'red',
        },
        {
            id: 6,
            missionIcon: 'fi fi-rr-users',
            missionTitle: 'Memory Care Work...',
            volunteerName: 'David Kumar',
            category: 'Workshop Help',
            patientName: 'Multiple p...',
            patientInitials: 'M',
            patientColor: '#3b82f6',
            assignedDate: 'Dec 21, 2025',
            missionDate: '2025-12-28',
            duration: '4 hours',
            rating: 4,
            status: 'Completed',
            statusColor: 'green',
        }
    ];

    myCompletedMissions = [
        {
            icon: 'fi fi-rr-phone-call',
            title: 'Weekly Phone Support Check-in',
            category: 'Phone Support',
            patientName: 'Elizabeth Martinez',
            patientInitial: 'E',
            patientColor: '#3b82f6',
            date: 'Jan 24, 2025',
            rating: 5
        },
        {
            icon: 'fi fi-rr-home',
            title: 'Home Visit – Medication Review',
            category: 'Home Visit',
            patientName: 'Margaret Thompson',
            patientInitial: 'M',
            patientColor: '#3b82f6',
            date: 'Dec 15, 2025',
            rating: 5
        },
        {
            icon: 'fi fi-rr-users',
            title: 'Memory Care Workshop Assistance',
            category: 'Workshop Help',
            patientName: 'Multiple participants',
            patientInitial: 'M',
            patientColor: '#3b82f6',
            date: 'Dec 28, 2025',
            rating: 4
        },
        {
            icon: 'fi fi-rr-brain',
            title: 'Cognitive Stimulation Session',
            category: 'Workshop Help',
            patientName: 'Harold Bennett',
            patientInitial: 'H',
            patientColor: '#3b82f6',
            date: 'Nov 20, 2025',
            rating: 5
        },
        {
            icon: 'fi fi-rr-car-side',
            title: 'Specialist Appointment Transport',
            category: 'Transport',
            patientName: 'George Nakamura',
            patientInitial: 'G',
            patientColor: '#3b82f6',
            date: 'Oct 8, 2025',
            rating: 4
        },
        {
            icon: 'fi fi-rr-home',
            title: 'Overnight Respite Care',
            category: 'Home Visit',
            patientName: 'Alice Fontaine',
            patientInitial: 'A',
            patientColor: '#3b82f6',
            date: 'Sep 15, 2025',
            rating: 5
        },
        {
            icon: 'fi fi-rr-users',
            title: 'Caregiver Training Workshop',
            category: 'Workshop Help',
            patientName: 'Multiple caregivers',
            patientInitial: 'M',
            patientColor: '#3b82f6',
            date: 'Aug 22, 2025',
            rating: 5
        }
    ];

    constructor(
        public readonly authService: AuthService,
        private readonly volunteerService: VolunteerService
    ) { }

    ngOnInit(): void {
        // In a real application, fetch from the backend:
        // this.loadAssignments();
    }

    get filteredAssignments(): AssignmentHistory[] {
        let list = this.assignments;
        if (this.activeFilter !== 'All') {
            list = list.filter(a => a.status === this.activeFilter);
        }
        if (this.searchQuery) {
            const q = this.searchQuery.toLowerCase();
            list = list.filter(a =>
                a.missionTitle.toLowerCase().includes(q) ||
                a.patientName.toLowerCase().includes(q) ||
                a.volunteerName.toLowerCase().includes(q)
            );
        }
        return list;
    }

    setFilter(filter: string) {
        this.activeFilter = filter;
    }

    starsArray(rating: number): number[] {
        return Array.from({ length: 5 }, (_, i) => i);
    }

    isFilledStar(index: number, rating: number): boolean {
        return index < Math.floor(rating);
    }
}
