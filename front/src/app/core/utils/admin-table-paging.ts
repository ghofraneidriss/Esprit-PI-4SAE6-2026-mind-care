/** Default page size for admin data tables (fixed visual height). */
export const ADMIN_TABLE_PAGE_SIZE = 10;

export function slicePage<T>(items: T[], pageIndex: number, pageSize: number): T[] {
  const start = pageIndex * pageSize;
  return items.slice(start, start + pageSize);
}

export function totalPageCount(length: number, pageSize: number): number {
  return Math.max(1, Math.ceil(Math.max(0, length) / pageSize));
}

/** Pad the current page with `null` so the table always shows `pageSize` rows. */
export function padPageRows<T>(items: T[], pageSize: number): (T | null)[] {
  const out: (T | null)[] = items.map((x) => x);
  while (out.length < pageSize) out.push(null);
  return out;
}
