import { Component, EventEmitter, Input, Output } from '@angular/core';
import { SearchReport } from '../lost-item.model';

@Component({
  selector: 'app-search-report-card',
  standalone: false,
  templateUrl: './search-report-card.html',
  styleUrls: ['./search-report-card.css'],
})
export class SearchReportCardComponent {
  @Input() report!: SearchReport;
  @Output() editClicked = new EventEmitter<SearchReport>();
  @Output() deleteClicked = new EventEmitter<number>();

  getResultClass(result?: string): string {
    switch (result) {
      case 'FOUND': return 'badge bg-success';
      case 'PARTIALLY_FOUND': return 'badge bg-warning text-dark';
      case 'NOT_FOUND': return 'badge bg-danger';
      default: return 'badge bg-secondary';
    }
  }

  getStatusClass(status?: string): string {
    switch (status) {
      case 'OPEN': return 'badge bg-primary';
      case 'CLOSED': return 'badge bg-secondary';
      case 'ESCALATED': return 'badge bg-warning text-dark';
      default: return 'badge bg-secondary';
    }
  }
}
