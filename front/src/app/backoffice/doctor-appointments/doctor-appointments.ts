import { Component, OnInit } from '@angular/core';
import { PatientProfileService } from '../../frontoffice/patient-profile/patient-profile.service';
import { AppointmentService, Appointment } from '../../frontoffice/appointment/appointment.service';
import { AuthService, AuthUser } from '../../frontoffice/auth/auth.service';
import { ActivatedRoute, Router } from '@angular/router';
import { ChangeDetectorRef } from '@angular/core';

declare var google: any;

@Component({
  selector: 'app-doctor-appointments',
  standalone: false,
  templateUrl: './doctor-appointments.html',
  styleUrls: ['./doctor-appointments.css'],
})
export class DoctorAppointments implements OnInit {
  appointments: Appointment[] = [];
  filteredAppointments: Appointment[] = [];
  usersMap: Map<number, AuthUser> = new Map();

  loggedUser: AuthUser | null = null;
  role: string = '';

  filter: string = 'all';
  minScore: number | null = null;
  maxScore: number | null = null;
  sortByScore: boolean = false;

  filterDate: string = '';
  filterPatientId: number | null = null;
  filterUrgent: string = '';

  filterOptionsDates: string[] = [];
  filterOptionsPatients: number[] = [];

  isLoading: boolean = true;
  errorMessage: string = '';

  dismissedAlertIds: Set<number> = new Set<number>();

  // ─── Custom Angular Calendar ─────────────────────────────────────────────
  /** Current calendar view: DAY | WEEK | MONTH | AGENDA */
  calMode: 'DAY' | 'WEEK' | 'MONTH' | 'AGENDA' = 'MONTH';
  /** The reference date used to render the current calendar period */
  calCurrentDate: Date = new Date();
  /** Map of "YYYY-MM-DD" → list of appointments on that date */
  calAppointmentMap: Map<string, Appointment[]> = new Map();

  // ─── Google Calendar Integration ─────────────────────────────────────────
  // Note: These keys should normally be in environment.ts to avoid exposing them. 
  // User is aware and will rotate them after today's validation.
  private CLIENT_ID = '176577385927-lsna4hmpuno7ihbi9chuk5poff915dfi.apps.googleusercontent.com';
  private API_KEY = 'AIzaSyBuXyEJXGUNnFUhN3byOi74bTzfKvNpzgQ';
  private DISCOVERY_DOCS = ["https://www.googleapis.com/discovery/v1/apis/calendar/v3/rest"];
  // Utilisation de .events pour permettre la lecture ET l'écriture (ajout/modification de RDV)
  private SCOPES = "https://www.googleapis.com/auth/calendar.events";

  gapiInited = false;
  isAuthorized = false;
  googleEventsLoaded: Appointment[] = [];

  constructor(
    private appointmentService: AppointmentService,
    private authService: AuthService,
    private patientProfileService: PatientProfileService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.loggedUser = this.authService.getLoggedUser();
    this.role = this.authService.getLoggedRole();

    if (this.role !== 'DOCTOR' && this.role !== 'ADMIN') {
      this.router.navigate(['/auth/login']);
      return;
    }

    this.route.url.subscribe(urlSegments => {
      const path = urlSegments.map(s => s.path).join('/');
      if (path.includes('pending')) this.filter = 'pending';
      else if (path.includes('confirmed')) this.filter = 'confirmed';
      else if (path.includes('cancelled')) this.filter = 'cancelled';
      else this.filter = 'all';
      this.loadAppointments();
    });

    this.loadUsers();
    this.loadFilterOptions();

    // We use GIS now, so API is instantly available
    this.gapiInited = true;

    // Check if we already have an access token saved
    const savedToken = sessionStorage.getItem('google_access_token');
    if (savedToken) {
      this.isAuthorized = true;
      this.fetchGoogleEventsWithToken(savedToken);
    } else {
      this.isAuthorized = false;
    }
  }

  loadFilterOptions(): void {
    const docId = this.role === 'ADMIN' ? undefined : this.loggedUser?.userId;
    this.appointmentService.getFilterDates(docId).subscribe({
      next: (dates: string[]) => {
        this.filterOptionsDates = dates;
        this.cdr.detectChanges();
      },
      error: (err: any) => console.error('[DoctorAppointments] Error fetching dates', err)
    });
    this.appointmentService.getFilterPatients(docId).subscribe({
      next: (patients: number[]) => {
        this.filterOptionsPatients = patients;
        this.cdr.detectChanges();
      },
      error: (err: any) => console.error('[DoctorAppointments] Error fetching patients', err)
    });
  }

  loadUsers(): void {
    this.authService.getAllUsers().subscribe({
      next: (users: AuthUser[]) => {
        users.forEach((u: AuthUser) => this.usersMap.set(u.userId, u));
        this.cdr.detectChanges();
      },
      error: (err: any) => console.error('[DoctorAppointments] Error fetching users', err)
    });
  }

  loadAppointments(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.loadUsers();

    if (!this.loggedUser) {
      this.errorMessage = 'Session expired. Please log in again.';
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    let urgentBool: boolean | undefined = undefined;
    if (this.filterUrgent === 'true') urgentBool = true;
    else if (this.filterUrgent === 'false') urgentBool = false;

    let dateStr: string | undefined = this.filterDate ? this.filterDate : undefined;

    if (this.role === 'ADMIN') {
      this.appointmentService.getFilteredAppointments(
        undefined,
        this.filterPatientId !== null ? this.filterPatientId : undefined,
        this.filter,
        urgentBool,
        dateStr,
        this.minScore !== null ? this.minScore : undefined,
        this.maxScore !== null ? this.maxScore : undefined,
        this.sortByScore
      ).subscribe({
        next: (data: Appointment[]) => {
          this.filteredAppointments = data ?? [];
          this.appointments = data ?? []; // For calendar
          this.buildCalendarMap();   // Update the custom calendar
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          console.error('[DoctorAppointments] Error fetching all appointments', err);
          this.errorMessage = 'Failed to load appointments. Please check the server is running.';
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
    } else {
      this.appointmentService.getFilteredAppointments(
        this.loggedUser.userId,
        this.filterPatientId !== null ? this.filterPatientId : undefined,
        this.filter,
        urgentBool,
        dateStr,
        this.minScore !== null ? this.minScore : undefined,
        this.maxScore !== null ? this.maxScore : undefined,
        this.sortByScore
      ).subscribe({
        next: (data: Appointment[]) => {
          this.filteredAppointments = data ?? [];
          this.appointments = data ?? []; // For calendar
          this.buildCalendarMap();   // Update the custom calendar
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          console.error('[DoctorAppointments] Error fetching doctor appointments', err);
          this.errorMessage = 'Failed to load your appointments. Please check the server is running.';
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
    }
  }

  /**
   * COMMENTAIRE POUR LE REPERAGE (Demande utilisateur) :
   * Appelle la méthode loadAppointments() qui délègue le filtrage en appelant le backend.
   * Le front-end ne fait plus de tri ou de filtrage local en mémoire.
   */
  applyFilter(): void {
    // Re-fetch from DB with updated filter args
    this.loadAppointments();
  }

  setFilter(f: string): void {
    this.filter = f;
    this.applyFilter();
  }

  // ─── GOOGLE CALENDAR ACTIONS ───────────────────────────────────────────

  handleAuthClick(): void {
    const tokenClient = google.accounts.oauth2.initTokenClient({
      client_id: this.CLIENT_ID,
      scope: this.SCOPES,
      callback: (tokenResponse: any) => {
        if (tokenResponse && tokenResponse.access_token) {
          sessionStorage.setItem('google_access_token', tokenResponse.access_token);
          this.isAuthorized = true;
          this.fetchGoogleEventsWithToken(tokenResponse.access_token);
        }
      }
    });
    tokenClient.requestAccessToken({ prompt: 'consent' });
  }

  handleSignoutClick(): void {
    sessionStorage.removeItem('google_access_token');
    this.isAuthorized = false;
    this.googleEventsLoaded = [];
    this.buildCalendarMap();
    this.cdr.detectChanges();
  }

  private async fetchGoogleEventsWithToken(accessToken: string): Promise<void> {
    const timeMin = new Date();
    timeMin.setFullYear(timeMin.getFullYear() - 1); // Get all from past 1 year

    const url = `https://www.googleapis.com/calendar/v3/calendars/primary/events?timeMin=${encodeURIComponent(timeMin.toISOString())}&showDeleted=false&singleEvents=true&maxResults=2500&orderBy=startTime`;

    try {
      const response = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Accept': 'application/json'
        }
      });

      if (response.status === 401 || response.status === 403) {
        this.handleSignoutClick();
        throw new Error('Unauthorized Google Access. Session expired');
      }

      if (!response.ok) throw new Error('Bad network response');

      const data = await response.json();
      const dbEvents = data.items || [];

      this.googleEventsLoaded = dbEvents
        .filter((e: any) => !(e.summary && e.summary.includes('Mind-Care')))
        .map((e: any): Appointment => {
          return {
            id: -1000 - Math.floor(Math.random() * 9000), // Fake ID
            patientId: -1,
            doctorId: this.loggedUser?.userId || 0,
            appointmentDate: e.start?.dateTime || e.start?.date,
            isUrgent: false,
            type: 'IN_PERSON' as any,
            category: 'DAILY_FOLLOW_UP' as any,
            status: 'CONFIRMED' as any,
            title: 'Google: ' + (e.summary || '(Sans Titre)')
          };
        });

      console.log('Google events injected into calendar view', this.googleEventsLoaded);
      this.errorMessage = '';
      this.buildCalendarMap(); // Rebuild with google events included
      this.cdr.detectChanges();

      // 🔄 TWO-WAY SYNC: Push missing local appointments to Google Calendar
      this.syncLocalToGoogle(accessToken, dbEvents);

    } catch (err: any) {
      console.error('Error fetching Google events', err);
      this.errorMessage = 'Google Calendar Error: ' + (err.message || 'sync failed');
      this.cdr.detectChanges();
    }
  }

  private async syncLocalToGoogle(accessToken: string, dbEvents: any[]): Promise<void> {
    for (const apt of this.appointments) {
      if (apt.status !== 'CONFIRMED' || !apt.appointmentDate) continue;

      const aptDate = new Date(apt.appointmentDate);
      const title = 'Mind-Care: ' + this.getPatientName(apt.patientId);

      const alreadyInGoogle = dbEvents.some((gEvent: any) => {
        if (!gEvent.start || (!gEvent.start.dateTime && !gEvent.start.date)) return false;
        const gDate = new Date(gEvent.start.dateTime || gEvent.start.date);
        const diffMinutes = Math.abs(gDate.getTime() - aptDate.getTime()) / 60000;
        return diffMinutes < 120 && gEvent.summary && gEvent.summary.includes('Mind-Care');
      });

      if (!alreadyInGoogle) {
        const endApt = new Date(aptDate.getTime() + 30 * 60000); // 30 min duration
        const newEvent = {
          summary: title,
          start: { dateTime: aptDate.toISOString() },
          end: { dateTime: endApt.toISOString() },
          description: `Type: ${apt.type}.\nConsultation ajoutée automatiquement par Mind-Care.`
        };

        try {
          await fetch(`https://www.googleapis.com/calendar/v3/calendars/primary/events`, {
            method: 'POST',
            headers: {
              'Authorization': `Bearer ${accessToken}`,
              'Content-Type': 'application/json'
            },
            body: JSON.stringify(newEvent)
          });
          console.log("Pushed local to Google:", title);
        } catch (e) {
          console.error("Failed to push to GC:", e);
        }
      }
    }
  }

  // ─── CUSTOM CALENDAR LOGIC ───────────────────────────────────────────────

  /**
   * Rebuilds the date-keyed map used by all calendar views.
   * Called automatically after every appointment load/update.
   */
  buildCalendarMap(): void {
    const map = new Map<string, Appointment[]>();

    // Merge standard appointments + Google loaded appointments (if any)
    const allAppointments = [...this.appointments, ...this.googleEventsLoaded];

    for (const apt of allAppointments) {
      if (!apt.appointmentDate) continue;
      const key = apt.appointmentDate.slice(0, 10);   // "YYYY-MM-DD" (already local time from backend)
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(apt);
    }
    this.calAppointmentMap = map;
  }

  /**
   * Converts a JS Date to a local "YYYY-MM-DD" key WITHOUT converting to UTC.
   * Using toISOString() would subtract the UTC offset (e.g. UTC+1 → shifts date back 1 day),
   * causing appointments to appear on the wrong calendar cell.
   */
  private localDateKey(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  /** Switch view mode and refresh */
  setCalMode(mode: 'DAY' | 'WEEK' | 'MONTH' | 'AGENDA'): void {
    this.calMode = mode;
    this.cdr.detectChanges();
  }

  /**
   * Navigate forward or backward by one period (month/week/day).
   * @param dir -1 = previous, +1 = next
   */
  navigateCal(dir: number): void {
    const d = new Date(this.calCurrentDate);
    if (this.calMode === 'MONTH') d.setMonth(d.getMonth() + dir);
    else if (this.calMode === 'WEEK') d.setDate(d.getDate() + dir * 7);
    else if (this.calMode === 'DAY') d.setDate(d.getDate() + dir);
    this.calCurrentDate = d;
    this.cdr.detectChanges();
  }

  /** Human-readable title shown above the calendar (e.g. "March 2026") */
  get calTitle(): string {
    if (this.calMode === 'AGENDA') return 'Upcoming Appointments';
    if (this.calMode === 'DAY') {
      return this.calCurrentDate.toLocaleDateString('en-US',
        { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });
    }
    return this.calCurrentDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  }

  /**
   * MONTH VIEW — returns a 6×7 grid of day cells.
   * The grid always starts on Monday (ISO week).
   */
  get calMonthDays(): { date: Date; otherMonth: boolean; isToday: boolean; apts: Appointment[] }[] {
    const year = this.calCurrentDate.getFullYear();
    const month = this.calCurrentDate.getMonth();
    const firstDay = new Date(year, month, 1);
    // Offset so grid starts on Monday (getDay: 0=Sun → offset 6, 1=Mon → 0, ...)
    const offset = firstDay.getDay() === 0 ? 6 : firstDay.getDay() - 1;
    const start = new Date(firstDay);
    start.setDate(start.getDate() - offset);

    const todayStr = new Date().toDateString();
    const cells: { date: Date; otherMonth: boolean; isToday: boolean; apts: Appointment[] }[] = [];

    for (let i = 0; i < 42; i++) {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      // localDateKey() avoids UTC conversion which would shift dates by 1 day in UTC+1
      const key = this.localDateKey(d);
      cells.push({
        date: d,
        otherMonth: d.getMonth() !== month,
        isToday: d.toDateString() === todayStr,
        apts: this.calAppointmentMap.get(key) || []
      });
    }
    return cells;
  }

  /**
   * WEEK VIEW — returns the 7 days (Mon–Sun) of the current week.
   */
  get calWeekDays(): { date: Date; isToday: boolean; apts: Appointment[] }[] {
    const curr = this.calCurrentDate;
    const dow = curr.getDay();
    const mon = new Date(curr);
    mon.setDate(curr.getDate() - (dow === 0 ? 6 : dow - 1));

    const todayStr = new Date().toDateString();
    const result: { date: Date; isToday: boolean; apts: Appointment[] }[] = [];

    for (let i = 0; i < 7; i++) {
      const d = new Date(mon);
      d.setDate(mon.getDate() + i);
      // localDateKey() avoids UTC conversion shifting
      const key = this.localDateKey(d);
      result.push({
        date: d,
        isToday: d.toDateString() === todayStr,
        apts: this.calAppointmentMap.get(key) || []
      });
    }
    return result;
  }

  /** DAY VIEW — appointments for the current calCurrentDate */
  get calDayApts(): Appointment[] {
    const key = this.localDateKey(this.calCurrentDate);   // local date, no UTC shift
    return this.calAppointmentMap.get(key) || [];
  }

  /**
   * AGENDA VIEW — all future appointment dates sorted chronologically,
   * each with its list of appointments.
   */
  get calAgendaGroups(): { label: string; apts: Appointment[] }[] {
    const today = this.localDateKey(new Date());           // local "today", no UTC shift
    const keys = Array.from(this.calAppointmentMap.keys()).filter(k => k >= today).sort();
    return keys.map(k => ({
      // Use T12:00:00 (midday) to avoid midnight UTC ambiguity in toLocaleDateString
      label: new Date(k + 'T12:00:00').toLocaleDateString('en-US',
        { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' }),
      apts: this.calAppointmentMap.get(k)!
    }));
  }

  /** Returns a CSS class based on appointment status (for colored dots/chips) */
  getAptStatusClass(status: string): string {
    switch (status) {
      case 'CONFIRMED': return 'apt-confirmed';
      case 'CANCELLED': return 'apt-cancelled';
      case 'RESCHEDULED': return 'apt-rescheduled';
      default: return 'apt-pending';
    }
  }

  // ─── APPOINTMENT ACTIONS ─────────────────────────────────────────────────

  confirmAppointment(apt: Appointment, event: Event): void {
    event.stopPropagation();
    if (!apt.id) return;
    this.appointmentService.confirmAppointment(apt.id).subscribe({
      next: (updated: Appointment) => {
        const idx = this.appointments.findIndex(a => a.id === updated.id);
        if (idx !== -1) this.appointments[idx] = updated;
        this.applyFilter();
        this.buildCalendarMap();
        this.cdr.detectChanges();
      },
      error: (err: any) => console.error('Error confirming appointment', err)
    });
  }

  cancelAppointment(apt: Appointment, event: Event): void {
    event.stopPropagation();
    if (!apt.id) return;
    if (!confirm(`Cancel appointment on ${new Date(apt.appointmentDate).toLocaleString()}? This cannot be undone.`)) return;
    this.appointmentService.cancelAppointment(apt.id).subscribe({
      next: (updated: Appointment) => {
        const idx = this.appointments.findIndex(a => a.id === updated.id);
        if (idx !== -1) this.appointments[idx] = updated;
        this.applyFilter();
        this.buildCalendarMap();
        this.cdr.detectChanges();
      },
      error: (err: any) => console.error('Error cancelling appointment', err)
    });
  }

  deleteAppointment(apt: Appointment, event: Event): void {
    event.stopPropagation();
    if (!apt.id) return;
    if (!confirm('Are you sure you want to delete this appointment? This action cannot be undone.')) return;
    this.appointmentService.deleteAppointment(apt.id).subscribe({
      next: () => {
        this.appointments = this.appointments.filter(a => a.id !== apt.id);
        this.applyFilter();
        this.buildCalendarMap();
      },
      error: (err: any) => console.error('Error deleting appointment', err)
    });
  }

  viewDetails(appointment: Appointment): void {
    this.router.navigate([`/admin/appointments/details/${appointment.id}`]);
  }

  viewPatientProfile(patientId: number, event: Event): void {
    event.stopPropagation();
    this.patientProfileService.getProfileByUserId(patientId).subscribe({
      next: (profile: any) => this.router.navigate([`/patient/profile/${profile.id}`]),
      error: (err: any) => console.error('Error loading patient profile', err)
    });
  }

  updateAppointment(apt: Appointment, event: Event): void {
    event.stopPropagation();
    console.log('Update appointment', apt);
  }

  get isAdmin(): boolean { return this.role === 'ADMIN'; }

  getPatientName(id: number): string {
    if (id === -1) return ''; // Handled by title in template
    const user = this.usersMap.get(id);
    return user ? `${user.firstName} ${user.lastName}` : `Patient #${id}`;
  }

  getDoctorName(id: number): string {
    const user = this.usersMap.get(id);
    return user ? `Dr. ${user.firstName} ${user.lastName}` : `Dr. #${id}`;
  }

  get pageTitle(): string {
    const lbl = this.filter === 'all' ? 'All' : this.filter === 'pending' ? 'Pending' :
      this.filter === 'confirmed' ? 'Confirmed' : 'Cancelled';
    return this.isAdmin ? `${lbl} Appointments — Admin View` : `${lbl} Appointments — My Schedule`;
  }

  get totalCount(): number { return this.appointments.length; }
  get pendingCount(): number { return this.appointments.filter(a => a.status === 'PENDING').length; }
  get confirmedCount(): number { return this.appointments.filter(a => a.status === 'CONFIRMED').length; }
  get cancelledCount(): number { return this.appointments.filter(a => a.status === 'CANCELLED').length; }

  goToDashboard(): void {
    this.router.navigate([this.isAdmin ? '/admin/dashboard' : '/doctor/dashboard']);
  }

  getScoreColor(score: number | undefined): string {
    if (score === undefined || score === null) return 'bg-secondary';
    if (score >= 7) return 'bg-danger text-white';
    if (score >= 4) return 'bg-warning text-dark';
    return 'bg-success text-white';
  }

  get actionRequiredApts(): Appointment[] {
    const now = new Date().getTime();
    return this.appointments.filter(a => {
      if (!a.id || this.dismissedAlertIds.has(a.id)) return false;
      if (a.status === 'RESCHEDULED') return true;
      // Show cancellations that are from today or in the future
      if (a.status === 'CANCELLED') {
        if (!a.appointmentDate) return false;
        const aptDate = new Date(a.appointmentDate).getTime();
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        return aptDate >= today.getTime();
      }
      return false;
    }).sort((a, b) => {
      if (!a.appointmentDate) return 1;
      if (!b.appointmentDate) return -1;
      return new Date(b.appointmentDate).getTime() - new Date(a.appointmentDate).getTime();
    });
  }

  dismissAlert(apt: Appointment): void {
    if (apt.id) {
      this.dismissedAlertIds.add(apt.id);
      this.cdr.detectChanges();
    }
  }
}
