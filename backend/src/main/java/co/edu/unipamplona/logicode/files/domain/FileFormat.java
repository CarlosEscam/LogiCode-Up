package co.edu.unipamplona.logicode.files.domain;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Formatos aceptados (RNF-04, RN-21): PDF, Word, PowerPoint, imágenes, videos y archivos de PSeInt, Arduino y
 * Scratch. Cada formato sabe reconocer su contenido real, para no confiar solo en la extensión.
 */
public enum FileFormat {

    PDF("application/pdf", true, Family.PDF, "pdf"),
    DOC("application/msword", false, Family.OLE, "doc"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", false, Family.ZIP, "docx"),
    PPT("application/vnd.ms-powerpoint", false, Family.OLE, "ppt"),
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", false, Family.ZIP, "pptx"),
    PNG("image/png", true, Family.PNG, "png"),
    JPG("image/jpeg", true, Family.JPEG, "jpg", "jpeg"),
    MP4("video/mp4", true, Family.ISO_MEDIA, "mp4"),
    MOV("video/quicktime", true, Family.ISO_MEDIA, "mov"),
    PSC("text/plain; charset=UTF-8", false, Family.TEXT, "psc"),
    INO("text/plain; charset=UTF-8", false, Family.TEXT, "ino"),
    SB3("application/x.scratch.sb3", false, Family.ZIP, "sb3");

    public static final long MAX_BYTES = 500L * 1024 * 1024;
    public static final long MAX_VIDEO_BYTES = 1024L * 1024 * 1024;
    /** Bytes iniciales que se examinan para reconocer el contenido. */
    public static final int HEAD_BYTES = 8192;

    private enum Family { PDF, OLE, ZIP, PNG, JPEG, ISO_MEDIA, TEXT }

    private static final byte[] OLE_SIGNATURE = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1,
            (byte) 0xB1, 0x1A, (byte) 0xE1};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] ZIP_SIGNATURE = {'P', 'K', 0x03, 0x04};
    private static final List<String> QUICKTIME_ATOMS = List.of("moov", "mdat", "wide", "free", "skip", "pnot");

    private final String contentType;
    private final boolean previewable;
    private final Family family;
    private final List<String> extensions;

    FileFormat(String contentType, boolean previewable, Family family, String... extensions) {
        this.contentType = contentType;
        this.previewable = previewable;
        this.family = family;
        this.extensions = List.of(extensions);
    }

    /** Formato según la extensión del nombre original, sin distinguir mayúsculas. */
    public static Optional<FileFormat> fromFilename(String filename) {
        if (filename == null) {
            return Optional.empty();
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return Optional.empty();
        }
        String extension = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(f -> f.extensions.contains(extension)).findFirst();
    }

    public String contentType() {
        return contentType;
    }

    /** Se puede mostrar dentro de la página (RF-25): PDF, imágenes y videos. */
    public boolean isPreviewable() {
        return previewable;
    }

    public boolean isVideo() {
        return family == Family.ISO_MEDIA;
    }

    public boolean isZipContainer() {
        return family == Family.ZIP;
    }

    public long maxBytes() {
        return isVideo() ? MAX_VIDEO_BYTES : MAX_BYTES;
    }

    public List<String> extensions() {
        return extensions;
    }

    /**
     * Comprueba que el contenido corresponde al formato (RNF-04, "tipo real").
     *
     * @param head primeros bytes del archivo (hasta {@link #HEAD_BYTES})
     * @param zipEntries nombres de las entradas si el archivo es un ZIP (DOCX, PPTX, SB3); vacío en otro caso
     */
    public boolean matchesContent(byte[] head, Set<String> zipEntries) {
        return switch (family) {
            case PDF -> indexOf(head, "%PDF-".getBytes(StandardCharsets.US_ASCII), 1024) >= 0;
            case OLE -> startsWith(head, OLE_SIGNATURE);
            case PNG -> startsWith(head, PNG_SIGNATURE);
            case JPEG -> head.length >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8
                    && (head[2] & 0xFF) == 0xFF;
            case ISO_MEDIA -> isIsoMedia(head);
            case TEXT -> isText(head);
            case ZIP -> startsWith(head, ZIP_SIGNATURE) && zipEntries.contains(requiredZipEntry());
        };
    }

    private String requiredZipEntry() {
        return switch (this) {
            case DOCX -> "word/document.xml";
            case PPTX -> "ppt/presentation.xml";
            case SB3 -> "project.json";
            default -> throw new IllegalStateException(name() + " no es un contenedor ZIP");
        };
    }

    private boolean isIsoMedia(byte[] head) {
        if (head.length < 12) {
            return false;
        }
        String box = new String(head, 4, 4, StandardCharsets.US_ASCII);
        if (box.equals("ftyp")) {
            return true;
        }
        return this == MOV && QUICKTIME_ATOMS.contains(box);
    }

    private static boolean isText(byte[] head) {
        if (head.length == 0) {
            return false;
        }
        for (byte b : head) {
            if (b == 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        return data.length >= prefix.length && Arrays.equals(data, 0, prefix.length, prefix, 0, prefix.length);
    }

    private static int indexOf(byte[] data, byte[] needle, int limit) {
        int end = Math.min(data.length, limit) - needle.length;
        for (int i = 0; i <= end; i++) {
            if (Arrays.equals(data, i, i + needle.length, needle, 0, needle.length)) {
                return i;
            }
        }
        return -1;
    }
}
