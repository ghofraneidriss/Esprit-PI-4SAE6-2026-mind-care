import { AppNotification } from '../services/notification.service';

function stripActorPrefix(n: AppNotification): string {
  let m = (n.message || '').trim();
  const actor = n.actorName?.trim();
  if (actor && m.startsWith(actor)) {
    m = m.slice(actor.length).replace(/^\s*[·•]\s*/, '').trim();
  }
  return m;
}

function parenForReactionLabel(label: string): string {
  const u = label.trim().toUpperCase().replace(/\s+/g, '');
  const map: Record<string, string> = {
    LIKE: 'like',
    LOVE: 'love',
    CARE: 'care',
    HAHA: 'haha',
    WOW: 'wow',
    SAD: 'sad',
    ANGRY: 'angry',
    DISLIKE: 'dislike',
  };
  return map[u] || label.trim().toLowerCase();
}

function emojiForReactionLabel(label: string): string {
  const u = label.trim().toUpperCase().replace(/\s+/g, '');
  const map: Record<string, string> = {
    LIKE: '👍',
    LOVE: '❤️',
    CARE: '🤗',
    HAHA: '😄',
    WOW: '😮',
    SAD: '😢',
    ANGRY: '😠',
    DISLIKE: '👎',
  };
  return map[u] || '👍';
}

function polishReactionTail(tail: string): string {
  const legacy = /^reacted with "([^"]+)"\s+on your post\s*·\s*"([^"]*)"\s*$/;
  const m = tail.match(legacy);
  if (m) {
    const label = m[1];
    const title = m[2];
    return `reacted to your post "${title}" (${parenForReactionLabel(label)}) ${emojiForReactionLabel(label)}`;
  }
  return tail;
}

function polishCommentTails(text: string): string {
  let out = text;
  out = out.replace(
    /^commented on a thread you follow · "/,
    'left a comment on a thread you follow — "'
  );
  out = out.replace(/^commented on your post · "/, 'left a new comment on your post — "');
  return out;
}

function polishRatingTail(text: string): string {
  const m = text.match(/^rated your post "([^"]+)" \((\d+) stars?\)$/);
  if (m) {
    const title = m[1];
    const stars = m[2];
    return `rated your post "${title}" · ${stars}/5 stars`;
  }
  const m2 = text.match(/^rated your post "([^"]+)" \((\d+) star\)$/);
  if (m2) {
    return `rated your post "${m2[1]}" · ${m2[2]}/5 stars`;
  }
  return text;
}

/** Legacy: "is now following your post · \"title\"" */
function polishFollowTail(text: string): string {
  const legacy = /^is now following your post\s*·\s*"([^"]*)"\s*$/;
  const m = text.match(legacy);
  if (m) {
    return `followed your post "${m[1]}"`;
  }
  return text;
}

/**
 * Text after the actor name (when the message starts with actorName).
 */
export function notificationTailForDisplay(n: AppNotification): string {
  const tail = stripActorPrefix(n);
  const k = n.eventKind || '';
  if (k === 'PATIENT_LOCATION_SHARED') {
    return polishLocationShareTail(tail);
  }
  if (k === 'FORUM_REACTION') {
    return polishReactionTail(tail);
  }
  if (k === 'FORUM_RATING') {
    return polishRatingTail(tail);
  }
  if (k === 'FORUM_THREAD_FOLLOW') {
    return polishFollowTail(tail);
  }
  return polishCommentTails(tail);
}

/**
 * Full message when there is no actor highlight (system / anonymous).
 */
function polishLocationShareTail(tail: string): string {
  let t = tail.trim();
  t = t.replace(/shared their live location at [\d\-T:.]+/i, 'shared their live position');
  t = t.replace(/\s*\.?\s*Tap to open directions in Google Maps\.?/i, '');
  t = t.replace(/\s+/g, ' ').trim();
  if (/^shared their live position\.?$/i.test(t)) {
    return 'shared their live position.';
  }
  return t.endsWith('.') ? t : t + '.';
}

export function notificationFullMessageForDisplay(n: AppNotification): string {
  const raw = (n.message || '').trim();
  const k2 = n.eventKind || '';
  if (k2 === 'PATIENT_LOCATION_SHARED') {
    return polishLocationShareTail(raw);
  }
  if (k2 === 'FORUM_REACTION') {
    return polishReactionTail(raw);
  }
  if (k2 === 'FORUM_RATING') {
    return polishRatingTail(raw);
  }
  if (k2 === 'FORUM_THREAD_FOLLOW') {
    return polishFollowTail(raw);
  }
  return polishCommentTails(raw);
}
