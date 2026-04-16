import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';
import { GameResult } from './game-result.service';
import { MovementAlert } from './patient-movement.service';
import { Incident } from '../models/incident.model';
import {
  CLINICAL_MOVEMENT_ALERTS_CRITICAL_THRESHOLD,
  isCriticalClinicalMovementAlertCount,
  movementRiskBand,
  MOVEMENT_RISK_LABEL_EN,
  MovementRiskBand
} from '../constants/critical-care.constants';

export interface PatientClinicalPdfPayload {
  patientName: string;
  patientId: number;
  email?: string;
  phone?: string;
  safeZoneExitAlerts: MovementAlert[];
  allMovementAlerts?: MovementAlert[];
  gameResults: GameResult[];
  incidents: Incident[];
}

/** MindCare PDF palette — teal / slate (readable on print) */
const C = {
  teal: [13, 148, 136] as [number, number, number],
  tealDark: [15, 118, 110] as [number, number, number],
  slate900: [15, 23, 42] as [number, number, number],
  slate700: [51, 65, 85] as [number, number, number],
  slate500: [100, 116, 139] as [number, number, number],
  slate400: [148, 163, 184] as [number, number, number],
  bg: [248, 250, 252] as [number, number, number],
  bgAccent: [240, 253, 250] as [number, number, number],
  border: [226, 232, 240] as [number, number, number],
  riskLow: [34, 197, 94] as [number, number, number],
  riskMid: [249, 115, 22] as [number, number, number],
  riskHigh: [239, 68, 68] as [number, number, number],
  barTrack: [226, 232, 240] as [number, number, number]
};

@Injectable({ providedIn: 'root' })
export class PatientClinicalPdfService {
  private readonly logoPath = 'assets/branding/alzcare-logo-3d.png';

  async downloadClinicalSummary(payload: PatientClinicalPdfPayload): Promise<void> {
    const doc = new jsPDF({ unit: 'mm', format: 'a4' });
    const pageW = doc.internal.pageSize.getWidth();
    const margin = 14;
    let y = margin;

    const logoData = await this.loadLogoDataUrl();
    y = this.drawHeader(doc, margin, pageW, y, logoData);

    doc.setDrawColor(...C.border);
    doc.line(margin, y, pageW - margin, y);
    y += 6;

    y = this.drawPatientCard(doc, margin, pageW, y, payload);

    const allMovement =
      payload.allMovementAlerts && payload.allMovementAlerts.length > 0
        ? payload.allMovementAlerts
        : payload.safeZoneExitAlerts || [];
    const exits = allMovement.filter((a) => String(a.alertType).toUpperCase() === 'OUT_OF_SAFE_ZONE');
    const totalMovement = allMovement.length;
    const band = movementRiskBand(totalMovement);

    y = this.drawSnapshotBlock(doc, margin, pageW, y, payload, exits.length, totalMovement, band);

    y += 2;
    y = this.drawMovementSection(doc, margin, pageW, y, allMovement);

    y += 2;
    y = this.drawGamesSection(doc, margin, pageW, y, payload.gameResults || []);

    y += 2;
    y = this.drawIncidentsSection(doc, margin, pageW, y, payload.incidents || []);

    const footY = doc.internal.pageSize.getHeight() - 9;
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(6.5);
    doc.setTextColor(...C.slate400);
    doc.text(
      'MindCare — information support only. Medical decisions remain with the clinician.',
      margin,
      footY,
      { maxWidth: pageW - 2 * margin }
    );

    const safe = this.sanitizeFilePart(payload.patientName);
    doc.save(`mindcare-patient-${safe}-${payload.patientId}.pdf`);
  }

  private drawHeader(doc: jsPDF, margin: number, pageW: number, y: number, logoData: string | null): number {
    const titleX = margin + (logoData ? 15 : 0);
    if (logoData) {
      try {
        doc.addImage(logoData, 'PNG', margin, y, 11, 11);
      } catch {
        /* ignore */
      }
    }
    doc.setFont('times', 'bold');
    doc.setFontSize(16);
    doc.setTextColor(...C.tealDark);
    doc.text('MindCare', titleX, y + 5);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(11);
    doc.setTextColor(...C.slate900);
    doc.text('Patient handout', titleX + 28, y + 5);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8);
    doc.setTextColor(...C.slate500);
    doc.text('Clinical summary — movement, cognition, incidents', titleX, y + 10.5);
    return y + 18;
  }

  private drawPatientCard(
    doc: jsPDF,
    margin: number,
    pageW: number,
    y: number,
    payload: PatientClinicalPdfPayload
  ): number {
    y = this.ensureSpace(doc, y, 32, margin);
    const h = 22;
    doc.setFillColor(...C.bg);
    doc.setDrawColor(...C.border);
    doc.roundedRect(margin, y, pageW - 2 * margin, h, 2, 2, 'FD');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(9);
    doc.setTextColor(...C.slate900);
    doc.text('Patient', margin + 4, y + 6);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9.5);
    doc.text(`${payload.patientName}   ·   ID ${payload.patientId}`, margin + 4, y + 12);
    doc.setFontSize(8);
    doc.setTextColor(...C.slate700);
    const contact = [payload.email, payload.phone].filter(Boolean).join('   ·   ');
    if (contact) {
      doc.text(contact, margin + 4, y + 17);
    }
    doc.setFontSize(7);
    doc.setTextColor(...C.slate400);
    doc.text(
      `Generated ${new Date().toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' })}`,
      pageW - margin - 4,
      y + 6,
      { align: 'right' }
    );
    return y + h + 5;
  }

  private drawSnapshotBlock(
    doc: jsPDF,
    margin: number,
    pageW: number,
    y: number,
    payload: PatientClinicalPdfPayload,
    exitCount: number,
    totalMovement: number,
    band: MovementRiskBand
  ): number {
    y = this.sectionTitle(doc, margin, pageW, y, 'Clinical snapshot');
    const innerW = pageW - 2 * margin - 8;
    const summaryLines = this.buildShortSummaryLines(payload, exitCount, totalMovement);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8.5);
    doc.setTextColor(...C.slate700);
    const snapLineGap = 4;
    for (const line of summaryLines) {
      const wrapped = doc.splitTextToSize(`• ${line}`, innerW);
      const blockH = wrapped.length * snapLineGap + 1;
      y = this.ensureSpace(doc, y, blockH + 2, margin);
      let cy = y;
      for (let i = 0; i < wrapped.length; i++) {
        doc.text(wrapped[i], margin + 4, cy);
        cy += snapLineGap;
      }
      y = cy + 0.5;
    }
    y += 3;

    y = this.ensureSpace(doc, y, 28, margin);
    const boxH = 22;
    doc.setFillColor(...C.bgAccent);
    doc.setDrawColor(...C.border);
    doc.roundedRect(margin, y, pageW - 2 * margin, boxH, 2, 2, 'FD');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8);
    doc.setTextColor(...C.tealDark);
    doc.text('Movement load (this export)', margin + 4, y + 6);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(14);
    doc.setTextColor(...C.slate900);
    doc.text(String(totalMovement), margin + 4, y + 14);
    doc.setFontSize(7.5);
    doc.setTextColor(...C.slate500);
    doc.text('total alerts', margin + 18, y + 14);

    const label = MOVEMENT_RISK_LABEL_EN[band];
    const rgb = band === 'low' ? C.riskLow : band === 'medium' ? C.riskMid : C.riskHigh;
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8);
    doc.setTextColor(...rgb);
    doc.text(`Risk band: ${label}`, margin + 50, y + 8);

    this.drawRiskBar(doc, margin + 50, y + 11, 75, 4, band);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(6.5);
    doc.setTextColor(...C.slate400);
    doc.text(
      `Scale: Low ≤10 · Medium 11–20 · High >20 alerts (informational)`,
      margin + 50,
      y + 18.5
    );

    return y + boxH + 6;
  }

  private drawRiskBar(
    doc: jsPDF,
    x: number,
    y: number,
    w: number,
    h: number,
    active: MovementRiskBand
  ): void {
    const seg = w / 3;
    const bands: MovementRiskBand[] = ['low', 'medium', 'high'];
    const fade = (rgb: [number, number, number], dim: boolean): [number, number, number] =>
      dim ? rgb.map((c) => Math.round(c + (255 - c) * 0.82)) as [number, number, number] : rgb;
    for (let i = 0; i < 3; i++) {
      const full = bands[i] === active;
      const base = bands[i] === 'low' ? C.riskLow : bands[i] === 'medium' ? C.riskMid : C.riskHigh;
      doc.setFillColor(...fade(base, !full));
      doc.rect(x + i * seg, y, seg - 0.3, h, 'F');
    }
  }

  private sectionTitle(doc: jsPDF, margin: number, pageW: number, y: number, title: string): number {
    y = this.ensureSpace(doc, y, 14, margin);
    doc.setFillColor(...C.teal);
    doc.rect(margin, y, 1.2, 7, 'F');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(10);
    doc.setTextColor(...C.slate900);
    doc.text(title, margin + 4, y + 5);
    return y + 9;
  }

  private drawMovementSection(doc: jsPDF, margin: number, pageW: number, y: number, allMovement: MovementAlert[]): number {
    y = this.sectionTitle(doc, margin, pageW, y, 'Movement alerts (newest first)');
    const alertsSorted = [...allMovement].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    );
    const maxAlerts = 35;
    const colDate = margin + 2;
    const colType = margin + 38;
    const colNote = margin + 72;
    const noteW = pageW - colNote - margin - 2;

    y = this.ensureSpace(doc, y, 10, margin);
    doc.setFillColor(241, 245, 249);
    doc.rect(margin, y, pageW - 2 * margin, 6, 'F');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(6.5);
    doc.setTextColor(...C.slate500);
    doc.text('DATE / TIME', colDate, y + 4);
    doc.text('TYPE', colType, y + 4);
    doc.text('DETAIL', colNote, y + 4);
    y += 8;

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(7.5);
    doc.setTextColor(...C.slate700);
    for (let i = 0; i < Math.min(alertsSorted.length, maxAlerts); i++) {
      const a = alertsSorted[i];
      const row = this.oneLineAlertNote(a);
      const wrapped = doc.splitTextToSize(row, noteW);
      const detailLineGap = 3.9;
      const rowH = Math.max(5, wrapped.length * detailLineGap + 1);
      y = this.ensureSpace(doc, y, rowH + 2, margin);
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(7);
      doc.setTextColor(...C.slate700);
      doc.text(this.fmtDate(a.createdAt), colDate, y + 3.5);
      doc.setFont('helvetica', 'bold');
      doc.setFontSize(6.5);
      doc.setTextColor(...C.tealDark);
      const typeLine = this.friendlyType(a.alertType);
      doc.text(typeLine.slice(0, 22), colType, y + 3.5);
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(7.5);
      doc.setTextColor(...C.slate700);
      let ny = y + 3.5;
      for (let wi = 0; wi < wrapped.length; wi++) {
        doc.text(wrapped[wi], colNote, ny);
        ny += detailLineGap;
      }
      y += rowH;
    }
    if (alertsSorted.length > maxAlerts) {
      y = this.ensureSpace(doc, y, 6, margin);
      doc.setFontSize(7);
      doc.setTextColor(...C.slate400);
      doc.text(
        `+ ${alertsSorted.length - maxAlerts} older alert(s) — full list in MindCare.`,
        margin + 2,
        y
      );
      y += 5;
    }
    return y;
  }

  private drawGamesSection(doc: jsPDF, margin: number, pageW: number, y: number, gameResults: GameResult[]): number {
    y = this.sectionTitle(doc, margin, pageW, y, 'Brain games (recent sessions)');
    const games = [...gameResults].sort(
      (a, b) => new Date(b.completedAt || 0).getTime() - new Date(a.completedAt || 0).getTime()
    );
    if (!games.length) {
      y = this.ensureSpace(doc, y, 8, margin);
      doc.setFont('helvetica', 'italic');
      doc.setFontSize(8.5);
      doc.setTextColor(...C.slate400);
      doc.text('No game results in this export.', margin + 2, y);
      return y + 6;
    }
    const barW = 55;
    const barH = 3.5;
    for (const g of games.slice(0, 12)) {
      y = this.ensureSpace(doc, y, 14, margin);
      const pct = g.maxScore > 0 ? Math.round((g.score / g.maxScore) * 100) : 0;
      const title = (g.activityTitle || g.activityType || 'Activity').slice(0, 42);
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(7.5);
      doc.setTextColor(...C.slate700);
      doc.text(`${this.fmtDate(g.completedAt)}`, margin + 2, y + 3);
      doc.text(title, margin + 36, y + 3);
      doc.setFillColor(...C.barTrack);
      doc.rect(margin + 100, y, barW, barH, 'F');
      doc.setFillColor(...C.teal);
      doc.rect(margin + 100, y, (barW * Math.min(100, pct)) / 100, barH, 'F');
      doc.setFont('helvetica', 'bold');
      doc.setFontSize(7);
      doc.setTextColor(...C.slate900);
      doc.text(`${pct}%`, margin + 100 + barW + 3, y + 3);
      if (g.riskLevel) {
        doc.setFont('helvetica', 'normal');
        doc.setFontSize(6.5);
        doc.setTextColor(...C.slate400);
        doc.text(String(g.riskLevel), margin + 100 + barW + 16, y + 3);
      }
      y += 8;
    }
    if (games.length > 12) {
      y = this.ensureSpace(doc, y, 6, margin);
      doc.setFontSize(7);
      doc.setTextColor(...C.slate400);
      doc.text(`+ ${games.length - 12} more session(s) in the app.`, margin + 2, y);
      y += 5;
    }
    return y;
  }

  private drawIncidentsSection(doc: jsPDF, margin: number, pageW: number, y: number, incidents: Incident[]): number {
    y = this.sectionTitle(doc, margin, pageW, y, 'Incidents (recent notes)');
    const incs = [...incidents].sort(
      (a, b) => new Date(String(b.incidentDate)).getTime() - new Date(String(a.incidentDate)).getTime()
    );
    if (!incs.length) {
      y = this.ensureSpace(doc, y, 8, margin);
      doc.setFont('helvetica', 'italic');
      doc.setFontSize(8.5);
      doc.setTextColor(...C.slate400);
      doc.text('No incident notes in this export.', margin + 2, y);
      return y + 6;
    }

    const innerPad = 3;
    const lineHeight = 4.2;
    const textMaxW = pageW - 2 * margin - 2 * innerPad;

    for (const inc of incs.slice(0, 10)) {
      const desc = (inc.description || '').replace(/\s+/g, ' ').trim();
      const descClean = desc.length > 200 ? `${desc.slice(0, 197)}…` : desc;
      const sev = String(inc.severityLevel || '—').toUpperCase().slice(0, 14);

      doc.setFontSize(7.5);
      const wrapped = doc.splitTextToSize(descClean || '—', textMaxW);

      const headerBaselineFromTop = innerPad + 5;
      const gapHeaderToBody = 5;
      const boxH = headerBaselineFromTop + gapHeaderToBody + wrapped.length * lineHeight + innerPad + 2;

      y = this.ensureSpace(doc, y, boxH + 3, margin);

      doc.setFillColor(...C.bg);
      doc.setDrawColor(...C.border);
      doc.roundedRect(margin, y, pageW - 2 * margin, boxH, 1.5, 1.5, 'FD');

      const xLeft = margin + innerPad;
      const xRight = pageW - margin - innerPad;
      const headerY = y + headerBaselineFromTop;

      doc.setFont('helvetica', 'bold');
      doc.setFontSize(7);
      doc.setTextColor(...C.slate500);
      doc.text(this.fmtDate(inc.incidentDate), xLeft, headerY);

      doc.setTextColor(...C.tealDark);
      doc.text(sev, xRight, headerY, { align: 'right' });

      doc.setFont('helvetica', 'normal');
      doc.setFontSize(7.5);
      doc.setTextColor(...C.slate700);

      let lineY = headerY + gapHeaderToBody;
      for (let i = 0; i < wrapped.length; i++) {
        doc.text(wrapped[i], xLeft, lineY);
        lineY += lineHeight;
      }

      y += boxH + 3;
    }
    return y;
  }

  private buildShortSummaryLines(
    payload: PatientClinicalPdfPayload,
    exitCount: number,
    totalMovement: number
  ): string[] {
    const lines: string[] = [];
    if (isCriticalClinicalMovementAlertCount(totalMovement)) {
      lines.push(
        `Movement activity: ${totalMovement} persisted alerts (above ${CLINICAL_MOVEMENT_ALERTS_CRITICAL_THRESHOLD}). Review supervision, environment, and device settings.`
      );
    } else if (totalMovement > 0) {
      lines.push(
        `${totalMovement} movement alert(s), including ${exitCount} “left safe zone” type. Not in the highest band; still review if clinically indicated.`
      );
    } else {
      lines.push('No movement alerts in this export file.');
    }

    const games = payload.gameResults || [];
    if (games.length) {
      let sum = 0;
      let n = 0;
      for (const g of games) {
        if (g.maxScore > 0) {
          sum += g.score / g.maxScore;
          n++;
        }
      }
      const avg = n ? Math.round((sum / n) * 100) : null;
      if (avg != null) {
        lines.push(`Cognitive games: mean score ${avg}% across ${games.length} session(s).`);
      }
    } else {
      lines.push('No quiz or game scores in this export.');
    }

    const incs = payload.incidents || [];
    if (incs.length) {
      lines.push(`${incs.length} incident note(s) — see section below.`);
    } else {
      lines.push('No incident notes in this export.');
    }

    return lines;
  }

  private friendlyType(t: string | undefined): string {
    const u = String(t || '').toUpperCase();
    const m: Record<string, string> = {
      OUT_OF_SAFE_ZONE: 'Left safe zone',
      LEFT_REGISTERED_HOME: 'Left home',
      IMMOBILE_TOO_LONG: 'Immobility',
      GPS_NO_DATA: 'No GPS',
      RAPID_OR_UNUSUAL_MOVEMENT: 'Unusual move',
    };
    return m[u] || u.replace(/_/g, ' ').toLowerCase();
  }

  /**
   * One line per alert; URLs removed; simulation wording kept professional (no “Demo test”).
   */
  private oneLineAlertNote(a: MovementAlert): string {
    let s = (a.message || '').trim();
    s = s.replace(/https:\/\/[^\s)]+/g, '').trim();
    s = s.replace(/\s*[—–-]\s*Position\s*\(patient\):\s*([0-9.,\s]+)/i, ' @ $1');
    s = s.replace(/^Test alert:\s*/i, '').replace(/^Simulated alert:\s*/i, '').trim();
    s = s.replace(/^The patient left all safe zones\.?\s*/i, 'Outside all safe zones. ');
    if (/simulated safe-zone exit/i.test(s)) {
      s = s.replace(/simulated safe-zone exit/gi, 'Safe-zone exit (simulation)').trim();
    }
    if (/simulated prolonged immobility/i.test(s)) {
      s = s.replace(/simulated prolonged immobility/gi, 'Immobility (simulation)').trim();
    }
    if (/simulated abrupt movement/i.test(s)) {
      s = s.replace(/simulated abrupt movement/gi, 'Rapid movement (simulation)').trim();
    }
    if (/No GPS data for/i.test(s)) {
      s = s.replace(/No GPS data for[^\n.]*/i, 'No GPS signal (no fix)').trim();
    }
    if (/Patient immobile for more than/i.test(s)) {
      s = s.replace(/Patient immobile for more than[^\n.]*/i, 'Prolonged immobility in zone').trim();
    }
    if (/Abrupt movement detected/i.test(s) || /Odd speed/i.test(s)) {
      s = s.replace(/\s+/g, ' ').trim();
    }
    if (!s) return '—';
    return s.length > 120 ? `${s.slice(0, 117)}…` : s;
  }

  private ensureSpace(doc: jsPDF, y: number, needMm: number, margin: number): number {
    const maxY = doc.internal.pageSize.getHeight() - margin - needMm;
    if (y > maxY) {
      doc.addPage();
      return margin + 8;
    }
    return y;
  }

  private fmtDate(raw: string | Date | undefined | null): string {
    if (raw == null) return '—';
    const d = typeof raw === 'string' || raw instanceof Date ? new Date(raw) : null;
    if (!d || Number.isNaN(d.getTime())) return '—';
    return d.toLocaleString('en-GB', { dateStyle: 'short', timeStyle: 'short' });
  }

  private sanitizeFilePart(name: string): string {
    return (name || 'patient').replace(/[^a-zA-Z0-9]+/g, '-').replace(/^-|-$/g, '').toLowerCase() || 'patient';
  }

  private async loadLogoDataUrl(): Promise<string | null> {
    try {
      const res = await fetch(this.logoPath);
      if (!res.ok) return null;
      const blob = await res.blob();
      return await new Promise((resolve) => {
        const r = new FileReader();
        r.onloadend = () => resolve(r.result as string);
        r.readAsDataURL(blob);
      });
    } catch {
      return null;
    }
  }
}
