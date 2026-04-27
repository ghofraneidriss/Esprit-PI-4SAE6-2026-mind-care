import { MatDialogConfig } from '@angular/material/dialog';
import { Category, Post } from '../../core/services/forum.service';

export type ForumPostFormDialogData = { post?: Post; categories?: Category[] };

/**
 * Compact “Facebook-style” composer: centered card, not fullscreen.
 * Same helper for dashboard, post list, and list-post routes.
 */
export function forumPostFormDialogConfig(data: ForumPostFormDialogData): MatDialogConfig<ForumPostFormDialogData> {
  return {
    data,
    width: 'min(920px, 98vw)',
    maxWidth: '100vw',
    maxHeight: '92vh',
    panelClass: ['post-form-modal', 'post-form-modal--fb'],
    backdropClass: 'post-form-backdrop-opaque',
    autoFocus: 'first-tabbable',
    hasBackdrop: true,
  };
}
