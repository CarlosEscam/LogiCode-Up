package co.edu.unipamplona.logicode.content.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Material de un tema (RF-07): un archivo cargado o un enlace externo, público o privado (RF-08). No se borra: se
 * archiva, y el archivo se conserva al menos el tiempo de retención (RN-21).
 */
@Entity
@Table(name = "material")
public class Material {

    @Id
    private UUID id;

    @Column(name = "topic_id", nullable = false, updatable = false)
    private UUID topicId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private MaterialKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    @Column(nullable = false)
    private int position;

    @Column(name = "file_id", updatable = false)
    private UUID fileId;

    @Column(name = "external_url", updatable = false)
    private String externalUrl;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Material() {
    }

    private Material(UUID topicId, String title, String description, MaterialKind kind, Visibility visibility,
            int position, UUID fileId, String externalUrl, UUID createdBy, Instant now) {
        this.id = UUID.randomUUID();
        this.topicId = topicId;
        this.title = title;
        this.description = description;
        this.kind = kind;
        this.visibility = visibility;
        this.position = position;
        this.fileId = fileId;
        this.externalUrl = externalUrl;
        this.createdBy = createdBy;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Material file(UUID topicId, String title, String description, MaterialKind kind,
            Visibility visibility, int position, UUID fileId, UUID createdBy, Instant now) {
        if (kind.isLink() || fileId == null) {
            throw new IllegalArgumentException("Un material de tipo " + kind + " requiere un archivo");
        }
        return new Material(topicId, title, description, kind, visibility, position, fileId, null, createdBy, now);
    }

    public static Material link(UUID topicId, String title, String description, Visibility visibility,
            int position, String externalUrl, UUID createdBy, Instant now) {
        if (externalUrl == null) {
            throw new IllegalArgumentException("Un enlace requiere una dirección");
        }
        return new Material(topicId, title, description, MaterialKind.LINK, visibility, position, null, externalUrl,
                createdBy, now);
    }

    public void update(String title, String description, Visibility visibility, Instant now) {
        this.title = title;
        this.description = description;
        changeVisibility(visibility, now);
        this.updatedAt = now;
    }

    public void changeVisibility(Visibility visibility, Instant now) {
        if (this.visibility != visibility) {
            this.visibility = visibility;
            this.updatedAt = now;
        }
    }

    public void moveTo(int position, Instant now) {
        if (this.position != position) {
            this.position = position;
            this.updatedAt = now;
        }
    }

    public void archive(Instant now) {
        this.archivedAt = now;
        this.updatedAt = now;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public boolean isPublic() {
        return visibility == Visibility.PUBLIC;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public MaterialKind getKind() {
        return kind;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public int getPosition() {
        return position;
    }

    public UUID getFileId() {
        return fileId;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
