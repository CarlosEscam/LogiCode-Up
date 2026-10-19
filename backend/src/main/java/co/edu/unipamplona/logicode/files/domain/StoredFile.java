package co.edu.unipamplona.logicode.files.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Metadatos de un archivo cargado. El contenido vive en el almacenamiento de archivos bajo una clave aleatoria
 * que nunca se expone; el archivo se conserva al menos hasta {@code retainUntil} (RN-21, RNF-12).
 */
@Entity
@Table(name = "stored_file")
public class StoredFile {

    @Id
    private UUID id;

    @Column(name = "storage_key", nullable = false, updatable = false)
    private String storageKey;

    @Column(name = "original_name", nullable = false, updatable = false)
    private String originalName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private FileFormat format;

    @Column(name = "content_type", nullable = false, updatable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Column(nullable = false, updatable = false)
    private String sha256;

    @Column(name = "uploaded_by", updatable = false)
    private UUID uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "retain_until", nullable = false)
    private Instant retainUntil;

    protected StoredFile() {
    }

    public StoredFile(String storageKey, String originalName, FileFormat format, long sizeBytes, String sha256,
            UUID uploadedBy, Instant uploadedAt, Instant retainUntil) {
        this.id = UUID.randomUUID();
        this.storageKey = storageKey;
        this.originalName = originalName;
        this.format = format;
        this.contentType = format.contentType();
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
        this.retainUntil = retainUntil;
    }

    public UUID getId() {
        return id;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalName() {
        return originalName;
    }

    public FileFormat getFormat() {
        return format;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public Instant getRetainUntil() {
        return retainUntil;
    }
}
