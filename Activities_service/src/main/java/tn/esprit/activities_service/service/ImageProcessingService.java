package tn.esprit.activities_service.service;

import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Redimensionne et compresse les images avant stockage LONGBLOB (JPEG).
 * Limite la taille finale pour limiter les erreurs MySQL / Hibernate sur LONGBLOB.
 */
@Service
public class ImageProcessingService {

    private static final int MAX_INPUT_BYTES = 20 * 1024 * 1024;
    private static final int MAX_OUTPUT_BYTES = 900 * 1024;
    private static final int MIN_DIMENSION = 384;
    private static final float JPEG_QUALITY = 0.82f;

    public byte[] resizeAndCompressToJpeg(byte[] raw) throws IOException {
        if (raw == null || raw.length == 0) {
            throw new IllegalArgumentException("Empty image file.");
        }
        if (raw.length > MAX_INPUT_BYTES) {
            throw new IllegalArgumentException(
                    "Image file is too large. Please choose a smaller image or use another file.");
        }
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(raw));
        if (src == null) {
            throw new IllegalArgumentException(
                    "Unsupported or invalid image format. Please use a JPEG or PNG image.");
        }
        BufferedImage rgb = toRgb(src);
        int maxDim = 1024;
        while (maxDim >= MIN_DIMENSION) {
            BufferedImage scaled = scaleDown(rgb, maxDim);
            float q = JPEG_QUALITY;
            byte[] out = writeJpeg(scaled, q);
            while (out.length > MAX_OUTPUT_BYTES && q > 0.34f) {
                q -= 0.06f;
                out = writeJpeg(scaled, q);
            }
            if (out.length <= MAX_OUTPUT_BYTES) {
                return out;
            }
            maxDim = maxDim * 3 / 4;
        }
        throw new IllegalArgumentException(
                "Unable to compress this image enough for storage. Please use a smaller or simpler image.");
    }

    private static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, src.getWidth(), src.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private static BufferedImage scaleDown(BufferedImage src, int maxDim) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxDim && h <= maxDim) {
            return src;
        }
        double scale = Math.min((double) maxDim / w, (double) maxDim / h);
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));
        BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return dst;
    }

    private static byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }
}
