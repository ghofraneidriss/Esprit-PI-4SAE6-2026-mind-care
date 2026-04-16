/**
 * Client-side resize + JPEG compression before upload (server applies a second pass).
 * Reduces oversized uploads and avoids many gateway / DB issues.
 */

const MAX_DIMENSION = 1200;
const TARGET_MAX_BYTES = 750_000;

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error('Could not load this image. Please try another file (JPEG or PNG).'));
    img.src = src;
  });
}

function canvasToJpegBlob(canvas: HTMLCanvasElement, quality: number): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (b) => (b ? resolve(b) : reject(new Error('Image export failed. Please try another image.'))),
      'image/jpeg',
      quality
    );
  });
}

/**
 * Returns a JPEG File ready for multipart upload.
 */
export async function compressImageForUpload(file: File): Promise<File> {
  if (!file.type.startsWith('image/')) {
    throw new Error('Please select an image file (JPEG or PNG).');
  }

  const url = URL.createObjectURL(file);
  try {
    const img = await loadImage(url);
    let w = img.naturalWidth;
    let h = img.naturalHeight;
    if (!w || !h) {
      throw new Error('Invalid image dimensions. Please try another file.');
    }

    const scale = Math.min(1, MAX_DIMENSION / Math.max(w, h));
    w = Math.max(1, Math.round(w * scale));
    h = Math.max(1, Math.round(h * scale));

    const canvas = document.createElement('canvas');
    canvas.width = w;
    canvas.height = h;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      throw new Error('Could not process this image in the browser. Please try another file.');
    }
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, w, h);
    ctx.drawImage(img, 0, 0, w, h);

    let q = 0.82;
    let blob = await canvasToJpegBlob(canvas, q);
    while (blob.size > TARGET_MAX_BYTES && q > 0.38) {
      q -= 0.07;
      blob = await canvasToJpegBlob(canvas, q);
    }

    if (blob.size > TARGET_MAX_BYTES * 1.4) {
      const w2 = Math.max(1, Math.round(w * 0.85));
      const h2 = Math.max(1, Math.round(h * 0.85));
      canvas.width = w2;
      canvas.height = h2;
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(0, 0, w2, h2);
      ctx.drawImage(img, 0, 0, w2, h2);
      q = 0.75;
      blob = await canvasToJpegBlob(canvas, q);
      while (blob.size > TARGET_MAX_BYTES && q > 0.38) {
        q -= 0.07;
        blob = await canvasToJpegBlob(canvas, q);
      }
    }

    const name = file.name.replace(/\.[^.]+$/, '') + '.jpg';
    return new File([blob], name, { type: 'image/jpeg' });
  } finally {
    URL.revokeObjectURL(url);
  }
}
