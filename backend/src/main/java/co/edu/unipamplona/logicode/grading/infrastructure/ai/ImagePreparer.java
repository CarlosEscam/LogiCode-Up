package co.edu.unipamplona.logicode.grading.infrastructure.ai;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import co.edu.unipamplona.logicode.grading.application.GradingProvider;

/**
 * Prepara una foto para el modelo: la endereza según la orientación EXIF del celular, la reduce para que el lado
 * mayor no pase de {@code maxSide} píxeles (en CPU cada píxel cuesta tiempo y memoria) y la envía como JPEG.
 */
class ImagePreparer {

    private static final float JPEG_QUALITY = 0.9f;

    private final int maxSide;

    ImagePreparer(int maxSide) {
        this.maxSide = maxSide;
    }

    byte[] prepare(byte[] original) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(original));
        } catch (IOException e) {
            image = null;
        }
        if (image == null) {
            throw new GradingProvider.GradingFailure("Una imagen de la entrega no se pudo abrir.");
        }
        return toJpeg(scaleAndRotate(image, exifOrientation(original)));
    }

    private BufferedImage scaleAndRotate(BufferedImage image, int orientation) {
        double scale = Math.min(1.0, (double) maxSide / Math.max(image.getWidth(), image.getHeight()));
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        boolean quarterTurn = orientation == 6 || orientation == 8;
        BufferedImage result = new BufferedImage(quarterTurn ? height : width, quarterTurn ? width : height,
                BufferedImage.TYPE_INT_RGB);
        AffineTransform transform = switch (orientation) {
            case 3 -> new AffineTransform(-1, 0, 0, -1, width, height);
            case 6 -> new AffineTransform(0, 1, -1, 0, height, 0);
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, width);
            default -> new AffineTransform();
        };
        transform.scale(scale, scale);
        Graphics2D g = result.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, result.getWidth(), result.getHeight());
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(image, transform, null);
        } finally {
            g.dispose();
        }
        return result;
    }

    private static byte[] toJpeg(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    /**
     * Orientación EXIF (1 a 8) de un JPEG; 1 si no tiene. Solo se corrigen los giros (3, 6 y 8), que son los que
     * producen las cámaras de los celulares; los reflejos se dejan como vienen.
     */
    static int exifOrientation(byte[] data) {
        if (data.length < 4 || (data[0] & 0xFF) != 0xFF || (data[1] & 0xFF) != 0xD8) {
            return 1;
        }
        int i = 2;
        while (i + 4 <= data.length && (data[i] & 0xFF) == 0xFF) {
            int marker = data[i + 1] & 0xFF;
            int length = u16(data, i + 2, true);
            if (marker == 0xDA || length < 2) {
                return 1;
            }
            int start = i + 4;
            if (marker == 0xE1 && start + 14 <= data.length && data[start] == 'E' && data[start + 1] == 'x'
                    && data[start + 2] == 'i' && data[start + 3] == 'f') {
                return orientationFromTiff(data, start + 6, Math.min(data.length, i + 2 + length));
            }
            i += 2 + length;
        }
        return 1;
    }

    private static int orientationFromTiff(byte[] data, int tiff, int end) {
        boolean bigEndian = data[tiff] == 'M';
        int ifd = tiff + (int) u32(data, tiff + 4, bigEndian);
        if (ifd + 2 > end) {
            return 1;
        }
        int entries = u16(data, ifd, bigEndian);
        for (int e = 0; e < entries; e++) {
            int entry = ifd + 2 + e * 12;
            if (entry + 12 > end) {
                return 1;
            }
            if (u16(data, entry, bigEndian) == 0x0112) {
                int value = u16(data, entry + 8, bigEndian);
                return value >= 1 && value <= 8 ? value : 1;
            }
        }
        return 1;
    }

    private static int u16(byte[] data, int at, boolean bigEndian) {
        int a = data[at] & 0xFF;
        int b = data[at + 1] & 0xFF;
        return bigEndian ? (a << 8) | b : (b << 8) | a;
    }

    private static long u32(byte[] data, int at, boolean bigEndian) {
        long high = u16(data, at, bigEndian);
        long low = u16(data, at + 2, bigEndian);
        return bigEndian ? (high << 16) | low : (low << 16) | high;
    }
}
