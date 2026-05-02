import { Component, OnInit, OnDestroy } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { VolunteerService } from '../../backoffice/volunteering/volunteer.service';
import { Mission } from '../../backoffice/volunteering/volunteering';

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
    selector: 'app-volunteer-missions',
    standalone: false,
    templateUrl: './volunteer-missions.html',
    styleUrls: ['./volunteer-missions.css']
})
export class VolunteerMissionsComponent implements OnInit, OnDestroy {
    searchQuery = '';
    activeFilter = 'All';
    loading = false;
    private readonly presenceTrackingEnabled = false;

    // Session & Presence tracking
    volunteerSessionPresence: any = null;
    onlineVolunteers: any[] = [];
    isSessionActive = false;

    // Stats
    totalAssignments = 0;
    activeAssignments = 0;
    completedMissionsCount = 0;
    averageRating = 0.0;

    assignments: AssignmentHistory[] = [];
    myCompletedMissions: any[] = [];

    constructor(
        public readonly authService: AuthService,
        private readonly volunteerService: VolunteerService
    ) { }

    ngOnInit(): void {
        this.loadAssignments();

        if (!this.presenceTrackingEnabled) {
            return;
        }

        const user = this.authService.getLoggedUser();

        // If user is already logged in, initialize session immediately
        if (user && user.role && user.role.toUpperCase() === 'VOLUNTEER') {
            this.initializeSession();
        }

        this.loadOnlineVolunteers();

        // Subscribe to login events to handle session initialization
        this.authService.loginCompleted$.subscribe((loggedInUser) => {
            if (loggedInUser.role && loggedInUser.role.toUpperCase() === 'VOLUNTEER') {
                this.initializeSession();
            }
        });

        // Subscribe to logout events to clean up session
        this.authService.logoutCompleted$.subscribe(() => {
            this.isSessionActive = false;
            this.volunteerService.disconnectWebSocket();
        });

        // Reload online volunteers every 30 seconds
        const presenceInterval = setInterval(() => this.loadOnlineVolunteers(), 30000);

        // Refresh session status every 20 seconds
        const sessionRefreshInterval = setInterval(() => this.refreshSessionStatus(), 20000);

        // Store interval IDs for cleanup
        (this as any).presenceInterval = presenceInterval;
        (this as any).sessionRefreshInterval = sessionRefreshInterval;
    }

    initializeSession(): void {
        const user = this.authService.getLoggedUser();
        if (!user) {
            console.warn('No logged-in user found');
            return;
        }

        console.log('Initializing volunteer session for:', user.firstName, user.lastName);

        // Mark this volunteer as online when component initializes
        this.volunteerService.markOnline(user.userId, `${user.firstName} ${user.lastName}`).subscribe({
            next: () => {
                this.isSessionActive = true;
                console.log('✅ Volunteer session marked as ONLINE');
                // Connect WebSocket for real-time presence updates
                this.volunteerService.connectWebSocket();
                // Load current presence info
                this.loadVolunteerPresence(user.userId);
            },
            error: (err) => {
                console.error('❌ Could not mark session as online:', err);
                // Still try to connect WebSocket even if marking online failed
                this.volunteerService.connectWebSocket();
            }
        });
    }

    loadVolunteerPresence(userId: number): void {
        this.volunteerService.getVolunteerPresence(userId).subscribe({
            next: (presence) => {
                this.volunteerSessionPresence = presence;
                this.isSessionActive = true;
                console.log('✅ Presence loaded:', presence);
            },
            error: (err) => {
                console.warn('⚠️ Could not load volunteer presence:', err);
                // If API call fails, still consider them online if markOnline succeeded
                if (this.isSessionActive) {
                    console.log('Session still active despite presence fetch error');
                }
            }
        });
    }

    loadOnlineVolunteers(): void {
        this.volunteerService.getOnlineVolunteers().subscribe({
            next: (volunteers) => {
                this.onlineVolunteers = volunteers;
                console.log('Online volunteers:', volunteers);
            },
            error: (err) => {
                console.warn('Could not load online volunteers:', err);
            }
        });
    }

    refreshSessionStatus(): void {
        const user = this.authService.getLoggedUser();
        if (user) {
            this.loadVolunteerPresence(user.userId);
        }
    }

    ngOnDestroy(): void {
        if (!this.presenceTrackingEnabled) {
            return;
        }

        const user = this.authService.getLoggedUser();
        if (user && this.isSessionActive) {
            // Mark as offline when leaving
            this.volunteerService.markOffline(user.userId).subscribe({
                next: () => {
                    console.log('✅ Volunteer session marked as OFFLINE');
                    this.volunteerService.disconnectWebSocket();
                },
                error: (err) => {
                    console.warn('⚠️ Could not mark session as offline:', err);
                }
            });
        }
        
        // Clean up intervals
        if ((this as any).presenceInterval) {
            clearInterval((this as any).presenceInterval);
        }
        if ((this as any).sessionRefreshInterval) {
            clearInterval((this as any).sessionRefreshInterval);
        }
    }

    getSessionStatus(): string {
        if (this.isSessionActive) {
            return '🟢 Online';
        }
        return '🔴 Offline';
    }

    getLastHeartbeat(): string {
        if (!this.volunteerSessionPresence?.lastHeartbeat) {
            return this.isSessionActive ? 'Just now' : 'N/A';
        }
        try {
            const date = new Date(this.volunteerSessionPresence.lastHeartbeat);
            return date.toLocaleTimeString();
        } catch (e) {
            return 'N/A';
        }
    }

    loadAssignments(): void {
        const user = this.authService.getLoggedUser();
        if (!user) return;

        this.loading = true;
        this.volunteerService.getAssignmentsByVolunteer(user.userId).subscribe({
            next: (data) => {
                this.assignments = data.map(a => this.mapToHistory(a));
                this.calculateStats();
                this.myCompletedMissions = this.assignments.filter(a => a.status === 'Completed');
                this.loading = false;
            },
            error: (err) => {
                console.error('Error loading assignments', err);
                this.loading = false;
            }
        });
    }

    startMission(id: number): void {
        this.updateStatus(id, 'IN_PROGRESS');
    }

    completeMission(id: number): void {
        this.updateStatus(id, 'COMPLETED');
    }

    private updateStatus(id: number, status: string): void {
        const assignment = { status: status };
        this.volunteerService.updateAssignment(id, assignment).subscribe({
            next: () => this.loadAssignments(),
            error: (err) => console.error('Error updating status', err)
        });
    }

    private mapToHistory(a: any): AssignmentHistory {
        const mission = a.mission || {};
        const status = this.mapStatus(a.status);

        return {
            id: a.id,
            missionIcon: this.getIconForCategory(mission.category),
            missionTitle: mission.title || 'Untitled Mission',
            volunteerName: 'Me',
            category: mission.category || 'General',
            patientName: mission.patientName || 'Anonymous',
            patientInitials: this.getInitials(mission.patientName || 'A'),
            patientColor: this.getRandomColor(mission.patientName || 'A'),
            assignedDate: a.assignedAt ? new Date(a.assignedAt).toLocaleDateString() : 'N/A',
            missionDate: mission.startDate || 'N/A',
            duration: mission.duration || 'N/A',
            rating: a.rating,
            status: status,
            statusColor: this.getStatusColor(status)
        };
    }

    private mapStatus(s: string): 'Assigned' | 'In Progress' | 'Cancelled' | 'Completed' {
        switch (s) {
            case 'ASSIGNED': return 'Assigned';
            case 'IN_PROGRESS': return 'In Progress';
            case 'CANCELLED': return 'Cancelled';
            case 'COMPLETED': return 'Completed';
            default: return 'Assigned';
        }
    }

    private getStatusColor(s: string): 'blue' | 'yellow' | 'red' | 'green' {
        switch (s) {
            case 'Assigned': return 'blue';
            case 'In Progress': return 'yellow';
            case 'Cancelled': return 'red';
            case 'Completed': return 'green';
            default: return 'blue';
        }
    }

    private getIconForCategory(cat: string): string {
        if (!cat) return 'fi fi-rr-document';
        cat = cat.toLowerCase();
        if (cat.includes('home')) return 'fi fi-rr-home';
        if (cat.includes('transport')) return 'fi fi-rr-car-side';
        if (cat.includes('phone')) return 'fi fi-rr-phone-call';
        if (cat.includes('workshop')) return 'fi fi-rr-users';
        if (cat.includes('errand')) return 'fi fi-rr-shopping-bag';
        return 'fi fi-rr-document';
    }

    private getInitials(name: string): string {
        return name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
    }

    private getRandomColor(seed: string): string {
        const colors = ['blue', 'indigo', 'purple', 'pink', 'red', 'orange', 'yellow', 'green', 'teal', 'cyan'];
        const hash = seed.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
        return colors[hash % colors.length];
    }

    private calculateStats(): void {
        this.totalAssignments = this.assignments.length;
        this.activeAssignments = this.assignments.filter(a => a.status === 'Assigned' || a.status === 'In Progress').length;
        this.completedMissionsCount = this.assignments.filter(a => a.status === 'Completed').length;

        const ratings = this.assignments.filter(a => a.rating).map(a => a.rating!);
        if (ratings.length > 0) {
            this.averageRating = ratings.reduce((a, b) => a + b, 0) / ratings.length;
            this.averageRating = Math.round(this.averageRating * 10) / 10;
        } else {
            this.averageRating = 0;
        }
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
                a.patientName.toLowerCase().includes(q)
            );
        }
        return list;
    }

    setFilter(filter: string) {
        this.activeFilter = filter;
    }

    downloadHistoryPdf(): void {
        const user = this.authService.getLoggedUser();
        const volunteerName = user ? `${user.firstName} ${user.lastName}`.trim() : 'Volunteer';
        const titleDate = this.getPdfDateStamp();
        const pages = this.buildHistoryPdfPages(volunteerName);
        this.downloadPdfFile(`Volunteer-History-${titleDate}.pdf`, `Volunteer-History-${titleDate}`, pages);
    }

    downloadAchievementPdf(m: AssignmentHistory): void {
        const user = this.authService.getLoggedUser();
        const volunteerName = user ? `${user.firstName} ${user.lastName}`.trim() : 'Volunteer';
        const titleDate = this.getPdfDateStamp();
        const safeMissionTitle = this.sanitizeFileName(m.missionTitle || 'Achievement');
        const pages = this.buildAchievementPdfPages(volunteerName, m);
        this.downloadPdfFile(`Volunteer-Achievement-${safeMissionTitle}-${titleDate}.pdf`, `Volunteer-Achievement-${safeMissionTitle}-${titleDate}`, pages);
    }

    private buildHistoryPdfPages(volunteerName: string): string[][] {
        const page: string[] = [
            'Volunteer Mission History Report',
            `Volunteer: ${volunteerName}`,
            `Generated at: ${new Date().toLocaleString()}`,
            '',
            `Total Assignments: ${this.totalAssignments}`,
            `Active Missions: ${this.activeAssignments}`,
            `Completed Missions: ${this.completedMissionsCount}`,
            `Average Rating: ${this.averageRating}/5`,
            '',
            'Mission | Category | Patient | Assigned Date | Mission Date | Duration | Rating | Status',
        ];

        const rows = this.filteredAssignments;
        if (rows.length === 0) {
            page.push('No assignments found.');
        } else {
            rows.forEach((a) => {
                page.push(
                    [
                        a.missionTitle,
                        a.category,
                        a.patientName,
                        a.assignedDate,
                        a.missionDate,
                        a.duration,
                        a.rating ?? '-',
                        a.status,
                    ].join(' | ')
                );
            });
        }

        return [page];
    }

    private buildAchievementPdfPages(volunteerName: string, mission: AssignmentHistory): string[][] {
        const rating = mission.rating ?? '-';
        return [[
            'Achieved Goal',
            `Mission: ${mission.missionTitle}`,
            `Volunteer: ${volunteerName}`,
            'Status: Completed',
            `Category: ${mission.category}`,
            `Patient: ${mission.patientName}`,
            `Assigned Date: ${mission.assignedDate}`,
            `Mission Date: ${mission.missionDate}`,
            `Duration: ${mission.duration}`,
            `Rating: ${rating}`,
            `Generated on: ${new Date().toLocaleString()}`,
        ]];
    }

    private downloadPdfFile(filename: string, title: string, pages: string[][]): void {
        const pdf = this.buildPdfDocument(title, pages);
        const blob = new Blob([pdf], { type: 'application/pdf' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.setTimeout(() => window.URL.revokeObjectURL(url), 1000);
    }

    private buildPdfDocument(title: string, pages: string[][]): string {
        const pageWidth = 595.28;
        const pageHeight = 841.89;
        const margin = 40;
        const fontSize = 12;
        const lineHeight = 16;
        const normalizedPages = this.paginatePdfLines(pages, Math.floor((pageHeight - margin * 2) / lineHeight));

        const objects: string[] = [];
        const pageCount = normalizedPages.length;
        const fontObjectId = 1;
        const pagesObjectId = 2 + pageCount * 2;
        const catalogObjectId = pagesObjectId + 1;

        objects.push('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>');

        const contentObjectIds: number[] = [];
        normalizedPages.forEach((pageLines, index) => {
            const content = this.buildPdfPageContent(pageLines, margin, pageHeight, fontSize, lineHeight);
            const contentObjectId = 2 + index;
            contentObjectIds.push(contentObjectId);
            objects.push(`<< /Length ${content.length} >>\nstream\n${content}\nendstream`);
        });

        normalizedPages.forEach((_, index) => {
            const pageObjectId = 2 + pageCount + index;
            objects.push(
                `<< /Type /Page /Parent ${pagesObjectId} 0 R /MediaBox [0 0 ${pageWidth} ${pageHeight}] /Resources << /Font << /F1 ${fontObjectId} 0 R >> >> /Contents ${contentObjectIds[index]} 0 R >>`
            );
            void pageObjectId;
        });

        const kids = Array.from({ length: pageCount }, (_, index) => `${2 + pageCount + index} 0 R`).join(' ');
        objects.push(`<< /Type /Pages /Kids [ ${kids} ] /Count ${pageCount} >>`);
        objects.push(`<< /Type /Catalog /Pages ${pagesObjectId} 0 R >>`);

        const header = '%PDF-1.4\n%codex\n';
        const body = objects.join('');
        let offset = header.length;
        const offsets = [0];
        for (const object of objects) {
            offsets.push(offset);
            offset += object.length;
        }

        const xrefStart = header.length + body.length;
        const xrefEntries = offsets
            .map((entry, index) => index === 0 ? '0000000000 65535 f ' : `${String(entry).padStart(10, '0')} 00000 n `)
            .join('\n');

        const trailer = [
            'xref',
            `0 ${objects.length + 1}`,
            xrefEntries,
            'trailer',
            `<< /Size ${objects.length + 1} /Root ${catalogObjectId} 0 R /Info << /Title (${this.escapePdfText(title)}) >> >>`,
            'startxref',
            `${xrefStart}`,
            '%%EOF',
            '',
        ].join('\n');

        return `${header}${body}${trailer}`;
    }

    private buildPdfPageContent(lines: string[], margin: number, pageHeight: number, fontSize: number, lineHeight: number): string {
        const startY = pageHeight - margin;
        const safeLines = lines.length ? lines : [''];
        const content: string[] = [
            'BT',
            `/F1 ${fontSize} Tf`,
            `${lineHeight} TL`,
            `${margin} ${startY} Td`,
        ];

        safeLines.forEach((line, index) => {
            content.push(`(${this.escapePdfText(line)}) Tj`);
            if (index < safeLines.length - 1) {
                content.push('T*');
            }
        });

        content.push('ET');
        return content.join('\n');
    }

    private paginatePdfLines(pages: string[][], maxLinesPerPage: number): string[][] {
        const result: string[][] = [];
        pages.forEach((page) => {
            let current: string[] = [];
            page.forEach((line) => {
                const wrappedLines = this.wrapPdfLine(line);
                wrappedLines.forEach((wrappedLine) => {
                    if (current.length >= maxLinesPerPage) {
                        result.push(current);
                        current = [];
                    }
                    current.push(wrappedLine);
                });
            });

            if (current.length) {
                result.push(current);
            }
        });

        return result.length ? result : [['']];
    }

    private wrapPdfLine(value: string, maxLength = 90): string[] {
        const text = this.toAscii(value);
        if (text.length <= maxLength) {
            return [text];
        }

        const chunks: string[] = [];
        let remaining = text;
        while (remaining.length > maxLength) {
            let breakAt = remaining.lastIndexOf(' ', maxLength);
            if (breakAt <= 0) {
                breakAt = maxLength;
            }
            chunks.push(remaining.slice(0, breakAt).trimEnd());
            remaining = remaining.slice(breakAt).trimStart();
        }
        if (remaining.length) {
            chunks.push(remaining);
        }
        return chunks;
    }

    private escapePdfText(value: string): string {
        return this.toAscii(value)
            .replace(/\\/g, '\\\\')
            .replace(/\(/g, '\\(')
            .replace(/\)/g, '\\)');
    }

    private toAscii(value: string): string {
        return String(value ?? '').replace(/[^\x20-\x7E]/g, '?');
    }

    private escapeHtml(value: string | number | null | undefined): string {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    private getPdfDateStamp(): string {
        const now = new Date();
        return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
    }

    private sanitizeFileName(value: string): string {
        return value
            .toLowerCase()
            .trim()
            .replace(/[^a-z0-9]+/g, '-')
            .replace(/^-+|-+$/g, '') || 'mission';
    }

    starsArray(rating: number): number[] {
        return Array.from({ length: 5 }, (_, i) => i);
    }

    isFilledStar(index: number, rating: number): boolean {
        return index < Math.floor(rating);
    }
}
