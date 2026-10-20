package co.edu.unipamplona.logicode.grading.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import co.edu.unipamplona.logicode.grading.application.GradingProvider;
import org.junit.jupiter.api.Test;

/** Las fotos de celular llegan enderezadas y reducidas; una imagen dañada es un fallo técnico. */
class ImagePreparerTest {

    @Test
    void rotatesByExifOrientationAndLimitsTheLongestSide() throws IOException {
        byte[] jpeg = withOrientation(jpeg(800, 400), 6);
        assertThat(ImagePreparer.exifOrientation(jpeg)).isEqualTo(6);
        BufferedImage prepared = ImageIO.read(new ByteArrayInputStream(new ImagePreparer(200).prepare(jpeg)));
        assertThat(prepared.getWidth()).isEqualTo(100);
        assertThat(prepared.getHeight()).isEqualTo(200);
    }

    @Test
    void smallImagesKeepTheirSize() throws IOException {
        byte[] png = OllamaGradingAdapterTest.png(120, 80);
        assertThat(ImagePreparer.exifOrientation(png)).isEqualTo(1);
        BufferedImage prepared = ImageIO.read(new ByteArrayInputStream(new ImagePreparer(1280).prepare(png)));
        assertThat(prepared.getWidth()).isEqualTo(120);
        assertThat(prepared.getHeight()).isEqualTo(80);
    }

    @Test
    void damagedImageIsATechnicalFailure() {
        assertThatThrownBy(() -> new ImagePreparer(100).prepare(new byte[] {1, 2, 3}))
                .isInstanceOf(GradingProvider.GradingFailure.class);
    }

    private static byte[] jpeg(int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpg", out);
        return out.toByteArray();
    }

    /** Inserta un segmento APP1 Exif mínimo (big-endian) con la etiqueta de orientación tras el SOI. */
    private static byte[] withOrientation(byte[] jpeg, int orientation) {
        byte[] tiff = {'M', 'M', 0, 42, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0,
                0, 0, 0, 0, 0, 0};
        byte[] header = {'E', 'x', 'i', 'f', 0, 0};
        int length = 2 + header.length + tiff.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(jpeg, 0, 2);
        out.write(0xFF);
        out.write(0xE1);
        out.write(length >> 8);
        out.write(length & 0xFF);
        out.write(header, 0, header.length);
        out.write(tiff, 0, tiff.length);
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
    }
}
