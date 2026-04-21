import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Appointment, AppointmentService, AppointmentStatus } from '../../frontoffice/appointment/appointment.service';
import { AuthService } from '../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-doctor-appointments',
  standalone: false,
  templateUrl: './doctor-appointments.html',
  styleUrls: ['./doctor-appointments.css']
})
export class DoctorAppointments implements OnInit {
  appointments: Appointment[] = [];
  filteredAppointments: Appointment[] = [];
  actionRequiredApts: Appointment[] = [];
  loggedUser: any = null;
  isLoading = true;
  errorMessage = '';
  filter = 'all';
  filterUrgent: string = 'all';
  filterPatientId: number | null = null;
  filterDate = '';
  filterOptionsPatients: number[] = [];
  filterOptionsDates: string[] = [];
  minScore: number | null = null;
  maxScore: number | null = null;
  sortByScore = false;
  calMode: 'DAY' | 'WEEK' | 'MONTH' | 'AGENDA' = 'MONTH';
  gapiInited = true;
  isAuthorized = false;
  calCurrentDate = new Date();
  calMonthDays: any[] = [];
  calWeekDays: any[] = [];
  calDayApts: Appointment[] = [];
  calAgendaGroups: Array<{ label: string; date: Date; apts: Appointment[] }> = [];
  patientNameCache = new Map<number, string>();
  doctorNameCache = new Map<number, string>();

  constructor(
    private readonly appointmentService: AppointmentService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef
  ) {}

  get isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  get totalCount(): number {
    return this.appointments.length;
  }

  get pendingCount(): number {
    return this.appointments.filter(a => (a.status || '').toUpperCase() === AppointmentStatus.PENDING).length;
  }

  get confirmedCount(): number {
    return this.appointments.filter(a => (a.status || '').toUpperCase() === AppointmentStatus.CONFIRMED).length;
  }

  get cancelledCount(): number {
    return this.appointments.filter(a => (a.status || '').toUpperCase() === AppointmentStatus.CANCELLED).length;
  }

  get calTitle(): string {
    const options: Intl.DateTimeFormatOptions = { month: 'long', year: 'numeric' };
    if (this.calMode === 'DAY') {
      return this.calCurrentDate.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });
    }
    if (this.calMode === 'WEEK') {
      return `Week of ${this.calCurrentDate.toLocaleDateString(undefined, options)}`;
    }
    if (this.calMode === 'AGENDA') {
      return 'Agenda';
    }
    return this.calCurrentDate.toLocaleDateString(undefined, options);
  }

  ngOnInit(): void {
    this.loggedUser = this.authService.getLoggedUser();
    this.loadAppointments();
  }

  goToDashboard(): void {
    this.router.navigate(['/admin']);
  }

  loadAppointments(): void {
    this.isLoading = true;
    this.errorMessage = '';

    const request$ = this.isAdmin
      ? this.appointmentService.getAppointments()
      : this.loggedUser?.userId
        ? this.appointmentService.getAppointmentsByDoctor(this.loggedUser.userId)
        : this.appointmentService.getAppointments();

    request$.subscribe({
      next: (data) => {
        this.appointments = data || [];
        this.filterOptionsPatients = Array.from(new Set(this.appointments.map(a => a.patientId))).sort((a, b) => a - b);
        this.filterOptionsDates = Array.from(new Set(this.appointments.map(a => a.appointmentDate?.slice(0, 10)).filter(Boolean))) as string[];
        this.applyFilter();
        this.refreshCalendar();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Unable to load appointments.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  dismissAlert(apt: Appointment): void {
    this.actionRequiredApts = this.actionRequiredApts.filter(a => a.id !== apt.id);
  }

  getPatientName(patientId: number): string {
    if (this.patientNameCache.has(patientId)) return this.patientNameCache.get(patientId)!;
    this.authService.getUserById(patientId).subscribe({
      next: (user) => {
        this.patientNameCache.set(patientId, `${user.firstName} ${user.lastName}`);
        this.cdr.detectChanges();
      }
    });
    return `Patient #${patientId}`;
  }

  getDoctorName(doctorId: number): string {
    if (this.doctorNameCache.has(doctorId)) return this.doctorNameCache.get(doctorId)!;
    this.authService.getUserById(doctorId).subscribe({
      next: (user) => {
        this.doctorNameCache.set(doctorId, `${user.firstName} ${user.lastName}`);
        this.cdr.detectChanges();
      }
    });
    return `Doctor #${doctorId}`;
  }

  confirmAppointment(apt: Appointment, event?: Event): void {
    event?.stopPropagation();
    if (!apt.id) return;
    this.appointmentService.confirmAppointment(apt.id).subscribe({
      next: () => this.loadAppointments()
    });
  }

  cancelAppointment(apt: Appointment, event?: Event): void {
    event?.stopPropagation();
    if (!apt.id) return;
    this.appointmentService.cancelAppointment(apt.id).subscribe({
      next: () => this.loadAppointments()
    });
  }

  deleteAppointment(apt: Appointment, event?: Event): void {
    event?.stopPropagation();
    if (!apt.id) return;
    if (confirm('Delete this appointment?')) {
      this.appointmentService.deleteAppointment(apt.id).subscribe({
        next: () => this.loadAppointments()
      });
    }
  }

  setCalMode(mode: 'DAY' | 'WEEK' | 'MONTH' | 'AGENDA'): void {
    this.calMode = mode;
    this.refreshCalendar();
  }

  handleAuthClick(): void {
    this.isAuthorized = true;
  }

  handleSignoutClick(): void {
    this.isAuthorized = false;
  }

  navigateCal(direction: number): void {
    const next = new Date(this.calCurrentDate);
    if (this.calMode === 'DAY') next.setDate(next.getDate() + direction);
    else if (this.calMode === 'WEEK') next.setDate(next.getDate() + (direction * 7));
    else next.setMonth(next.getMonth() + direction);
    this.calCurrentDate = next;
    this.refreshCalendar();
  }

  getDate(value: string | Date): Date {
    return new Date(value);
  }

  getAptStatusClass(status: string): string {
    return `status-${(status || 'PENDING').toLowerCase()}`;
  }

  setFilter(value: string): void {
    this.filter = value;
    this.applyFilter();
  }

  applyFilter(): void {
    const filtered = this.appointments.filter((apt) => {
      const matchesStatus = this.filter === 'all' || (apt.status || '').toLowerCase() === this.filter;
      const matchesUrgent = this.filterUrgent === 'all' || String(apt.isUrgent) === this.filterUrgent;
      const matchesPatient = this.filterPatientId == null || apt.patientId === this.filterPatientId;
      const matchesDate = !this.filterDate || (apt.appointmentDate || '').slice(0, 10) === this.filterDate;
      const matchesMinScore = (this.minScore == null || (apt.priorityScore ?? 0) >= this.minScore);
      const matchesMaxScore = (this.maxScore == null || (apt.priorityScore ?? 0) <= this.maxScore);
      return matchesStatus && matchesUrgent && matchesPatient && matchesDate && matchesMinScore && matchesMaxScore;
    });
    this.filteredAppointments = this.sortByScore
      ? [...filtered].sort((a, b) => (b.priorityScore ?? 0) - (a.priorityScore ?? 0))
      : filtered;
    this.actionRequiredApts = this.appointments.filter(a => a.status === AppointmentStatus.RESCHEDULED || a.status === AppointmentStatus.CANCELLED);
  }

  viewDetails(apt: Appointment): void {
    if (apt?.id) {
      this.router.navigate(['/admin/appointments', apt.id]);
    }
  }

  getScoreColor(score?: number): string {
    if (score == null) return 'bg-secondary-subtle text-secondary';
    if (score >= 80) return 'bg-danger-subtle text-danger';
    if (score >= 50) return 'bg-warning-subtle text-warning';
    return 'bg-success-subtle text-success';
  }

  stopPropagation(event: Event): void {
    event.stopPropagation();
  }

  private refreshCalendar(): void {
    const day = new Date(this.calCurrentDate);
    const start = new Date(day);
    start.setDate(day.getDate() - day.getDay() + 1);
    this.calWeekDays = Array.from({ length: 7 }, (_, i) => {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      return {
        date: d,
        isToday: this.isSameDay(d, new Date()),
        apts: this.appointments.filter(a => this.isSameDay(new Date(a.appointmentDate), d))
      };
    });

    const monthStart = new Date(day.getFullYear(), day.getMonth(), 1);
    const firstGridDay = new Date(monthStart);
    firstGridDay.setDate(monthStart.getDate() - ((monthStart.getDay() + 6) % 7));
    this.calMonthDays = Array.from({ length: 42 }, (_, i) => {
      const d = new Date(firstGridDay);
      d.setDate(firstGridDay.getDate() + i);
      return {
        date: d,
        otherMonth: d.getMonth() !== day.getMonth(),
        apts: this.appointments.filter(a => this.isSameDay(new Date(a.appointmentDate), d))
      };
    });

    this.calDayApts = this.appointments.filter(a => this.isSameDay(new Date(a.appointmentDate), new Date(this.calCurrentDate)));
    const groupedByDay = new Map<string, { date: Date; apts: Appointment[] }>();
    for (const apt of [...this.appointments].sort((a, b) => new Date(a.appointmentDate).getTime() - new Date(b.appointmentDate).getTime())) {
      const date = new Date(apt.appointmentDate);
      const key = date.toISOString().slice(0, 10);
      const existing = groupedByDay.get(key);
      if (existing) {
        existing.apts.push(apt);
      } else {
        groupedByDay.set(key, { date, apts: [apt] });
      }
    }
    this.calAgendaGroups = Array.from(groupedByDay.values()).map(group => ({
      ...group,
      label: group.date.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' }),
    }));
  }

  private isSameDay(a: Date, b: Date): boolean {
    return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
  }
}
