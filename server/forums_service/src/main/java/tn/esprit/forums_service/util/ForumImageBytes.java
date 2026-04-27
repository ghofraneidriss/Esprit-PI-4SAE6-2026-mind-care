package tn.esprit.forums_service.util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Keeps forum photo payloads under typical MySQL packet limits (LONGBLOB insert).
 */
public final class ForumImageBytes {

    /** Align with front compression target (~900 KiB). */
    public static final int MAX_ALLOWED = 900_000;

    private ForumImageBytes() {}

    public static byte[] ensureUnderLimit(byte[] data, String contentType) throws IOException {
        if (data == null || data.length == 0) {
            return data;
        }
        if (data.length <= MAX_ALLOWED) {
            return data;
        }
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(data));
        if (src == null) {
            throw new IllegalArgumentException(
                    "Image is too large and could not be decoded. Use JPEG or PNG, or reduce file size.");
        }
        BufferedImage rgb = toRgb(src);
        float maxSide = 1600f;
        for (int attempt = 0; attempt < 7; attempt++) {
            int w = rgb.getWidth();
            int h = rgb.getHeight();
            if (w <= 0 || h <= 0) {
                throw new IllegalArgumentException("Invalid image dimensions.");
            }
            float s = Math.min(1f, Math.min(maxSide / (float) w, maxSide / (float) h));
            int nw = Math.max(1, Math.round(w * s));
            int nh = Math.max(1, Math.round(h * s));
            BufferedImage scaled = scale(rgb, nw, nh);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(scaled, "jpg", out);
            byte[] jpeg = out.toByteArray();
            if (jpeg.length <= MAX_ALLOWED) {
                return jpeg;
            }
            maxSide *= 0.72f;
            if (maxSide < 120f) {
                break;
            }
        }
        throw new IllegalArgumentException(
                "Image remains too large after server compression. Choose a smaller photo.");
    }

    private static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    private static BufferedImage scale(BufferedImage src, int nw, int nh) {
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }
        return out;
    }
}
