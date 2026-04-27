import { Component, OnInit } from '@angular/core';
import { AppointmentService, Appointment } from '../../core/services/appointment.service';

@Component({
  selector: 'app-calendar',
  standalone: false,
  templateUrl: './calendar.html',
  styleUrls: ['./calendar.css'],
})
export class CalendarPage implements OnInit {

  appointments: Appointment[] = [];
  loading = true;
  showForm = false;
  editingId: number | null = null;

  today = new Date();
  currentYear = this.today.getFullYear();
  currentMonth = this.today.getMonth(); // 0-indexed

  readonly months = ['January','February','March','April','May','June',
                     'July','August','September','October','November','December'];
  readonly weekDays = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];

  readonly appointmentTypes = ['VISIT','ACTIVITY','MEDICAL','OTHER'];
  readonly colorMap: { [k: string]: string } = {
    VISIT: 'primary', ACTIVITY: 'success', MEDICAL: 'danger', OTHER: 'warning'
  };

  form: Appointment = this.emptyForm();
  calendarDays: { date: Date; isCurrentMonth: boolean }[] = [];

  constructor(private appointmentService: AppointmentService) {}

  ngOnInit(): void {
    this.buildCalendar();
    this.load();
  }

  load(): void {
    this.loading = true;
    this.appointmentService.getAll().subscribe({
      next: (data) => { this.appointments = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  buildCalendar(): void {
    const firstDay = new Date(this.currentYear, this.currentMonth, 1);
    const lastDay = new Date(this.currentYear, this.currentMonth + 1, 0);
    this.calendarDays = [];

    // Fill from previous month
    for (let i = 0; i < firstDay.getDay(); i++) {
      const d = new Date(firstDay);
      d.setDate(d.getDate() - (firstDay.getDay() - i));
      this.calendarDays.push({ date: d, isCurrentMonth: false });
    }

    // Current month days
    for (let d = 1; d <= lastDay.getDate(); d++) {
      this.calendarDays.push({ date: new Date(this.currentYear, this.currentMonth, d), isCurrentMonth: true });
    }

    // Fill remaining
    const remaining = 42 - this.calendarDays.length;
    for (let i = 1; i <= remaining; i++) {
      this.calendarDays.push({ date: new Date(this.currentYear, this.currentMonth + 1, i), isCurrentMonth: false });
    }
  }

  prevMonth(): void {
    if (this.currentMonth === 0) { this.currentMonth = 11; this.currentYear--; }
    else this.currentMonth--;
    this.buildCalendar();
  }

  nextMonth(): void {
    if (this.currentMonth === 11) { this.currentMonth = 0; this.currentYear++; }
    else this.currentMonth++;
    this.buildCalendar();
  }

  getAppointmentsForDay(date: Date): Appointment[] {
    return this.appointments.filter(a => {
      const d = new Date(a.startDate);
      return d.getFullYear() === date.getFullYear()
          && d.getMonth() === date.getMonth()
          && d.getDate() === date.getDate();
    });
  }

  isToday(date: Date): boolean {
    const t = new Date();
    return date.getFullYear() === t.getFullYear()
        && date.getMonth() === t.getMonth()
        && date.getDate() === t.getDate();
  }

  openForm(date?: Date): void {
    this.form = this.emptyForm();
    if (date) {
      const d = new Date(date);
      d.setHours(9, 0, 0, 0);
      this.form.startDate = d.toISOString().slice(0, 16);
    }
    this.editingId = null;
    this.showForm = true;
  }

  editAppointment(a: Appointment): void {
    this.form = { ...a, startDate: a.startDate?.slice(0, 16), endDate: a.endDate?.slice(0, 16) };
    this.editingId = a.id ?? null;
    this.showForm = true;
  }

  save(): void {
    const payload = { ...this.form, color: this.colorMap[this.form.type] || 'primary' };
    if (this.editingId) {
      this.appointmentService.update(this.editingId, payload).subscribe(() => { this.showForm = false; this.load(); });
    } else {
      this.appointmentService.create(payload).subscribe(() => { this.showForm = false; this.load(); });
    }
  }

  delete(id: number): void {
    if (!confirm('Delete this appointment?')) return;
    this.appointmentService.delete(id).subscribe(() => this.load());
  }

  private emptyForm(): Appointment {
    return { title: '', type: 'VISIT', startDate: '', color: 'primary' };
  }
}
