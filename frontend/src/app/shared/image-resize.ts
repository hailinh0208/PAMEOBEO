/** Longest side of a product photo once uploaded: sharp enough full screen, light enough for phones. */
const MAX_SIDE = 1600;
const JPEG_QUALITY = 0.85;

/**
 * Shrinks a photo straight off the phone/camera (often 4000px, 5-10 MB) before uploading it, and re-encodes it as
 * JPEG. Re-encoding also drops the EXIF metadata, such as the GPS position where the photo was taken.
 */
export async function resizeForUpload(file: File): Promise<Blob> {
  const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
  const scale = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height));
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(bitmap.width * scale);
  canvas.height = Math.round(bitmap.height * scale);
  const ctx = canvas.getContext('2d')!;
  ctx.fillStyle = '#fff'; // JPEG has no transparency: transparent PNG areas become white, not black
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();
  return new Promise((resolve, reject) =>
    canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('Không đọc được ảnh'))), 'image/jpeg', JPEG_QUALITY));
}
