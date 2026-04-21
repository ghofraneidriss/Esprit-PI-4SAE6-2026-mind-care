import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../frontoffice/auth/auth.service';
import { VolunteerService } from '../volunteering/volunteer.service';
import { createPdfBlob, downloadPdfBlob } from '../shared/pdf-utils';

interface VolunteerHistoryCard {
  id: number;
  icon: string;
  missionTitle: string;
  category: string;
  volunteerName: string;
  patientName: string;
  assignedDate: string;
  missionDate: string;
  duration: string;
  rating?: number;
  status: string;
  statusColor: string;
}

@Component({
  selector: 'app-home2',
  standalone: false,
  templateUrl: './home2.html',
  styleUrls: ['./home2.css'],
})
export class Home2 implements OnInit {
  recentAssignments: VolunteerHistoryCard[] = [];
  historyLoading = false;
  historyError = '';
  actionLoadingId: number | null = null;

  constructor(
    public authService: AuthService,
    private readonly volunteerService: VolunteerService
  ) { }

  get isVolunteer(): boolean {
    return this.authService.isVolunteer();
  }

  ngOnInit(): void {
    if (this.isVolunteer) {
      this.loadVolunteerHistory();
    }
  }

  loadVolunteerHistory(): void {
    const user = this.authService.getLoggedUser();
    if (!user?.userId) {
      this.recentAssignments = [];
      return;
    }

    this.historyLoading = true;
    this.historyError = '';

    this.volunteerService.getAssignmentsByVolunteer(user.userId).subscribe({
      next: (assignments) => {
        this.recentAssignments = (assignments ?? [])
          .map((assignment) => this.mapAssignment(assignment))
          .slice(0, 4);
        this.historyLoading = false;
      },
      error: (err) => {
        console.error('Failed to load volunteer history', err);
        this.historyError = 'Could not load your assignment history.';
        this.recentAssignments = [];
        this.historyLoading = false;
      },
    });
  }

  downloadCardPdf(card: VolunteerHistoryCard): void {
    if (card.status !== 'Completed') {
      return;
    }
    const user = this.authService.getLoggedUser();
    const volunteerName = user ? `${user.firstName} ${user.lastName}`.trim() : 'Volunteer';
    const title = `Volunteer-History-${this.sanitizeFileName(card.missionTitle)}-${this.getPdfDateStamp()}`;
    const pages = [[
      'Volunteer Assignment History',
      `Volunteer: ${volunteerName}`,
      `Mission: ${card.missionTitle}`,
      `Patient: ${card.patientName}`,
      `Category: ${card.category}`,
      `Status: ${card.status}`,
      `Assigned Date: ${card.assignedDate}`,
      `Mission Date: ${card.missionDate}`,
      `Duration: ${card.duration}`,
      `Rating: ${card.rating ?? '-'}`,
      `Generated on: ${new Date().toLocaleString()}`,
    ]];

    downloadPdfBlob(createPdfBlob(title, pages), `${title}.pdf`);
  }

  acceptMission(card: VolunteerHistoryCard): void {
    if (card.status !== 'Assigned') {
      return;
    }
    this.actionLoadingId = card.id;
    this.volunteerService.acceptAssignment(card.id).subscribe({
      next: () => {
        this.actionLoadingId = null;
        this.loadVolunteerHistory();
      },
      error: (err) => {
        console.error('Failed to accept assignment', err);
        this.historyError = 'Could not accept mission. Please try again.';
        this.actionLoadingId = null;
      },
    });
  }

  refuseMission(card: VolunteerHistoryCard): void {
    if (card.status !== 'Assigned') {
      return;
    }
    this.actionLoadingId = card.id;
    this.volunteerService.refuseAssignment(card.id).subscribe({
      next: () => {
        this.actionLoadingId = null;
        this.loadVolunteerHistory();
      },
      error: (err) => {
        console.error('Failed to refuse assignment', err);
        this.historyError = 'Could not refuse mission. Please try again.';
        this.actionLoadingId = null;
      },
    });
  }

  completeMission(card: VolunteerHistoryCard): void {
    if (card.status !== 'In Progress') {
      return;
    }
    this.actionLoadingId = card.id;
    this.volunteerService.completeAssignment(card.id).subscribe({
      next: () => {
        this.actionLoadingId = null;
        this.loadVolunteerHistory();
      },
      error: (err) => {
        console.error('Failed to complete assignment', err);
        this.historyError = 'Could not complete mission. Please try again.';
        this.actionLoadingId = null;
      },
    });
  }

  private mapAssignment(assignment: any): VolunteerHistoryCard {
    const mission = assignment?.mission ?? {};
    const category = mission.category || 'General';
    const status = this.normalizeStatus(assignment?.status);
    const volunteerName = this.getVolunteerName();

    return {
      id: assignment?.id ?? 0,
      icon: this.iconForCategory(category),
      missionTitle: mission.title || 'Untitled Mission',
      category,
      volunteerName,
      patientName: this.extractPatientName(mission.description),
      assignedDate: this.formatDate(assignment?.assignedAt),
      missionDate: this.formatDate(mission.startDate),
      duration: mission.duration || 'N/A',
      rating: assignment?.rating ?? undefined,
      status,
      statusColor: this.statusColor(status),
    };
  }

  private getVolunteerName(): string {
    const user = this.authService.getLoggedUser();
    return user ? `${user.firstName} ${user.lastName}`.trim() : 'Volunteer';
  }

  private extractPatientName(description?: string): string {
    if (!description) {
      return 'N/A';
    }
    const match = description.match(/patient\s*:\s*([^|]+)/i);
    return match?.[1]?.trim() || 'N/A';
  }

  private normalizeStatus(statusRaw?: string): string {
    const status = (statusRaw ?? '').toUpperCase();
    if (status === 'IN_PROGRESS') return 'In Progress';
    if (status === 'COMPLETED') return 'Completed';
    if (status === 'CANCELLED') return 'Cancelled';
    return 'Assigned';
  }

  private statusColor(status: string): string {
    if (status === 'Completed') return 'green';
    if (status === 'In Progress') return 'yellow';
    if (status === 'Cancelled') return 'red';
    return 'blue';
  }

  private iconForCategory(category?: string): string {
    const c = (category || '').toLowerCase();
    if (c.includes('home') || c.includes('visit')) return 'fi fi-rr-home';
    if (c.includes('transport') || c.includes('car')) return 'fi fi-rr-car-side';
    if (c.includes('phone') || c.includes('support')) return 'fi fi-rr-phone-call';
    if (c.includes('shop') || c.includes('errand') || c.includes('grocery')) return 'fi fi-rr-shopping-bag';
    if (c.includes('workshop') || c.includes('group')) return 'fi fi-rr-users';
    return 'fi fi-rr-checkbox';
  }

  private formatDate(value?: string): string {
    if (!value) return 'N/A';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }

  private sanitizeFileName(value: string): string {
    return value
      .toLowerCase()
      .trim()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '') || 'mission';
  }

  private getPdfDateStamp(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  }
}
