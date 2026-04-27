/**
 * Court signal sonore pour nouvelle notification (sans fichier audio).
 * Peut rester silencieux tant que l’utilisateur n’a pas interagi avec la page (politique navigateur).
 */
export function playNotificationChime(): void {
  if (typeof window === 'undefined') {
    return;
  }
  try {
    const AC = window.AudioContext || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!AC) {
      return;
    }
    const ctx = new AC();
    const o = ctx.createOscillator();
    const g = ctx.createGain();
    o.type = 'sine';
    o.frequency.setValueAtTime(880, ctx.currentTime);
    o.frequency.exponentialRampToValueAtTime(660, ctx.currentTime + 0.12);
    g.gain.setValueAtTime(0.0001, ctx.currentTime);
    g.gain.exponentialRampToValueAtTime(0.12, ctx.currentTime + 0.02);
    g.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.18);
    o.connect(g);
    g.connect(ctx.destination);
    o.start(ctx.currentTime);
    o.stop(ctx.currentTime + 0.2);
    o.onended = (): void => {
      void ctx.close().catch(() => {});
    };
  } catch {
    /* ignore */
  }
}
