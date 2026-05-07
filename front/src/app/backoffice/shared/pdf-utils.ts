export function createPdfBlob(title: string, pages: string[][]): Blob {
  const pageWidth = 595.28;
  const pageHeight = 841.89;
  const margin = 42;
  const fontSize = 12;
  const lineHeight = 16;
  const normalizedPages = paginatePdfLines(pages, Math.floor((pageHeight - 220) / lineHeight));

  const objectBodies: Record<number, string> = {};
  const pageCount = normalizedPages.length;
  const fontObjectId = 1;
  const boldFontObjectId = 2;
  const firstContentObjectId = 3;
  const firstPageObjectId = firstContentObjectId + pageCount;
  const pagesObjectId = firstPageObjectId + pageCount;
  const catalogObjectId = pagesObjectId + 1;
  const infoObjectId = catalogObjectId + 1;

  objectBodies[fontObjectId] = '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>';
  objectBodies[boldFontObjectId] = '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>';

  normalizedPages.forEach((pageLines, index) => {
    const content = buildPdfPageContent(pageLines, pageWidth, pageHeight, margin, fontSize, lineHeight);
    objectBodies[firstContentObjectId + index] = [
      `<< /Length ${content.length} >>`,
      'stream',
      content,
      'endstream',
    ].join('\n');
  });

  normalizedPages.forEach((_, index) => {
    const pageObjectId = firstPageObjectId + index;
    const contentObjectId = firstContentObjectId + index;
    objectBodies[pageObjectId] = [
      `<< /Type /Page /Parent ${pagesObjectId} 0 R /MediaBox [0 0 ${pageWidth} ${pageHeight}]`,
      `/Resources << /Font << /F1 ${fontObjectId} 0 R /F2 ${boldFontObjectId} 0 R >> >>`,
      `/Contents ${contentObjectId} 0 R >>`,
    ].join(' ');
  });

  const kids = Array.from({ length: pageCount }, (_, index) => `${firstPageObjectId + index} 0 R`).join(' ');
  objectBodies[pagesObjectId] = `<< /Type /Pages /Kids [ ${kids} ] /Count ${pageCount} >>`;
  objectBodies[catalogObjectId] = `<< /Type /Catalog /Pages ${pagesObjectId} 0 R >>`;
  objectBodies[infoObjectId] = `<< /Title (${escapePdfText(title)}) /Producer (MindCare) /Creator (MindCare) >>`;

  const maxObjectId = infoObjectId;
  const offsets: number[] = [];
  let body = '';

  for (let objectId = 1; objectId <= maxObjectId; objectId += 1) {
    offsets[objectId] = body.length;
    body += `${objectId} 0 obj\n${objectBodies[objectId]}\nendobj\n`;
  }

  const header = '%PDF-1.4\n%codex\n';
  const xrefOffset = header.length + body.length;

  const xrefLines = ['0000000000 65535 f '];
  for (let objectId = 1; objectId <= maxObjectId; objectId += 1) {
    xrefLines.push(`${String(header.length + offsets[objectId]).padStart(10, '0')} 00000 n `);
  }

  const trailer = [
    'xref',
    `0 ${maxObjectId + 1}`,
    xrefLines.join('\n'),
    'trailer',
    `<< /Size ${maxObjectId + 1} /Root ${catalogObjectId} 0 R /Info ${infoObjectId} 0 R >>`,
    'startxref',
    `${xrefOffset}`,
    '%%EOF',
    '',
  ].join('\n');

  return new Blob([`${header}${body}${trailer}`], { type: 'application/pdf' });
}

export function downloadPdfBlob(blob: Blob, filename: string): void {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => window.URL.revokeObjectURL(url), 1000);
}

function buildPdfPageContent(
  lines: string[],
  pageWidth: number,
  pageHeight: number,
  margin: number,
  fontSize: number,
  lineHeight: number
): string {
  const title = lines[0] || 'MindCare Report';
  const subtitle = lines[1] || '';
  const bodyLines = lines.slice(2);
  const topY = pageHeight - 52;
  const cardTop = pageHeight - 128;
  const cardBottom = 86;
  let currentY = pageHeight - 152;
  const safeLines = bodyLines.length ? bodyLines : ['No details available.'];
  const content: string[] = [
    'q',
    '0.97 0.98 1 rg',
    `0 0 ${pageWidth} ${pageHeight} re f`,
    '0.11 0.27 0.62 rg',
    `0 ${pageHeight - 86} ${pageWidth} 86 re f`,
    '0.15 0.36 0.84 rg',
    `0 ${pageHeight - 92} ${pageWidth} 6 re f`,
    '0.93 0.96 1 rg',
    `${margin} ${cardBottom} ${pageWidth - margin * 2} ${cardTop - cardBottom} re f`,
    '0.86 0.9 0.96 RG',
    '1 w',
    `${margin} ${cardBottom} ${pageWidth - margin * 2} ${cardTop - cardBottom} re S`,
    'BT',
    '/F2 22 Tf',
    '1 1 1 rg',
    `${margin} ${topY} Td`,
    `(${escapePdfText(title)}) Tj`,
    'ET',
    'BT',
    '/F1 10 Tf',
    '0.92 0.96 1 rg',
    `${margin} ${topY - 24} Td`,
    `(${escapePdfText(subtitle)}) Tj`,
    'ET',
  ];

  safeLines.forEach((line) => {
    if (currentY < 110) {
      return;
    }

    const row = line.includes(':') ? line.split(':', 2) : [line, ''];
    const label = row[0].trim();
    const value = row[1].trim();

    content.push('0.95 0.97 1 rg');
    content.push(`${margin} ${currentY - 14} ${pageWidth - margin * 2} 18 re f`);
    content.push('BT');
    content.push('/F2 12 Tf');
    content.push('0.08 0.18 0.38 rg');
    content.push(`${margin} ${currentY} Td`);
    content.push(`(${escapePdfText(label)}) Tj`);
    content.push('ET');

    if (value) {
      content.push('BT');
      content.push('/F1 12 Tf');
      content.push('0.17 0.24 0.35 rg');
      content.push(`${margin + 155} ${currentY} Td`);
      content.push(`(${escapePdfText(value)}) Tj`);
      content.push('ET');
    }

    currentY -= lineHeight + 4;
  });

  content.push('BT');
  content.push('/F1 9 Tf');
  content.push('0.47 0.55 0.68 rg');
  content.push(`${margin} 44 Td`);
  content.push('(Generated by MindCare) Tj');
  content.push('ET');
  content.push('BT');
  content.push('/F1 9 Tf');
  content.push('0.47 0.55 0.68 rg');
  content.push(`470 44 Td`);
  content.push('(Page 1 of 1) Tj');
  content.push('ET');
  content.push('Q');
  return content.join('\n');
}

function paginatePdfLines(pages: string[][], maxLinesPerPage: number): string[][] {
  const result: string[][] = [];
  pages.forEach((page) => {
    let current: string[] = [];
    page.forEach((line) => {
      const wrappedLines = wrapPdfLine(line);
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

function wrapPdfLine(value: string, maxLength = 90): string[] {
  const text = toAscii(value);
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

function escapePdfText(value: string): string {
  return toAscii(value)
    .replace(/\\/g, '\\\\')
    .replace(/\(/g, '\\(')
    .replace(/\)/g, '\\)');
}

function toAscii(value: string): string {
  return String(value ?? '').replace(/[^\x20-\x7E]/g, '?');
}
