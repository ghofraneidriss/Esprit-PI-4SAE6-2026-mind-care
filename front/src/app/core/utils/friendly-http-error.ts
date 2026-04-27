/**
 * Turns HTTP / backend errors into short, non-technical English for toasts.
 * Drops SQL, JDBC, Hibernate, and long stack-style text.
 */

function extractMessage(error: unknown): string {
  if (error == null) return '';
  if (typeof error === 'string') return error.trim();
  if (typeof error !== 'object') return '';
  const err = error as Record<string, unknown>;
  const nested = err['error'];
  if (typeof nested === 'string') return nested.trim();
  if (nested && typeof nested === 'object') {
    const m = (nested as { message?: unknown }).message;
    if (typeof m === 'string') return m.trim();
  }
  const msg = err['message'];
  if (typeof msg === 'string') return msg.trim();
  return '';
}

function looksTechnicalOrSql(s: string): boolean {
  const t = s.toLowerCase();
  if (t.length > 220) return true;
  if (
    /\b(insert into|update |delete from|select |jdbc|hibernate|sqlexception|sqlsyntax|could not execute statement|could not extract|statement \[\]|constraint \[|foreign key|deadlock|rollback)\b/i.test(
      t
    )
  ) {
    return true;
  }
  if (/column\s+['"`]?\w*['"`]?\s+cannot be null/i.test(t)) return true;
  if (/\b(values\s*\(|into\s+\w+\s*\(|longblob|varchar\(\d+\))\b/i.test(t)) return true;
  if (t.includes('org.hibernate') || t.includes('org.springframework.jdbc')) return true;
  if (t.includes('at ') && t.includes('.java:')) return true;
  return false;
}

/** Short messages from our APIs that are safe to show as-is. */
function isWhitelistedApiMessage(s: string): boolean {
  const t = s.trim();
  if (t.length < 3 || t.length > 160) return false;
  if (looksTechnicalOrSql(t)) return false;
  // Typical Spring / custom validation one-liners
  if (/^[A-Za-z0-9\s.,'’!?\-–—:()%]+$/u.test(t)) return true;
  return false;
}

/**
 * @param fallback User-facing English when the error is technical or unknown.
 */
export function friendlyHttpError(error: unknown, fallback: string): string {
  const raw = extractMessage(error);
  if (!raw) return fallback;
  if (looksTechnicalOrSql(raw)) return fallback;
  if (isWhitelistedApiMessage(raw)) {
    const c = raw.charAt(0).toUpperCase() + raw.slice(1);
    return c.endsWith('.') ? c : `${c}.`;
  }
  return fallback;
}
