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
 * Tema de un curso (RF-06), con su herramienta y su lugar en el orden del curso. Puede marcarse como tema de la
 * próxima clase (RF-09). No se borra: se archiva, y sus materiales dejan de mostrarse.
 */
@Entity
@Table(name = "topic")
public class Topic {

    @Id
    private UUID id;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tool tool;

    @Column(nullable = false)
    private int position;

    @Column(name = "next_class", nullable = false)
    private boolean nextClass;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Topic() {
    }

    public Topic(UUID courseId, String title, String description, Tool tool, int position, Instant now) {
        this.id = UUID.randomUUID();
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.tool = tool;
        this.position = position;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String title, String description, Tool tool, Instant now) {
        this.title = title;
        this.description = description;
        this.tool = tool;
        this.updatedAt = now;
    }

    public void moveTo(int position, Instant now) {
        if (this.position != position) {
            this.position = position;
            this.updatedAt = now;
        }
    }

    public void markNextClass(boolean nextClass, Instant now) {
        if (nextClass && isArchived()) {
            throw new IllegalStateException("Un tema archivado no puede ser la próxima clase");
        }
        if (this.nextClass != nextClass) {
            this.nextClass = nextClass;
            this.updatedAt = now;
        }
    }

    public void archive(Instant now) {
        this.nextClass = false;
        this.archivedAt = now;
        this.updatedAt = now;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Tool getTool() {
        return tool;
    }

    public int getPosition() {
        return position;
    }

    public boolean isNextClass() {
        return nextClass;
    }
}
