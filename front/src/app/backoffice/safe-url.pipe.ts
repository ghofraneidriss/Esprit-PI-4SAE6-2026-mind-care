import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

/**
 * Pipe that bypasses Angular's iframe URL sanitization.
 * Required to embed external URLs (Google Calendar) safely in <iframe> tags.
 *
 * Usage in template: [src]="url | safeUrl"
 */
@Pipe({
    name: 'safeUrl',
    standalone: false
})
export class SafeUrlPipe implements PipeTransform {
    constructor(private sanitizer: DomSanitizer) { }

    transform(url: string): SafeResourceUrl {
        return this.sanitizer.bypassSecurityTrustResourceUrl(url);
    }
}
