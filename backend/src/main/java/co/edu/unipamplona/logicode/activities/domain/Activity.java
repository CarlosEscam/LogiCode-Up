package co.edu.unipamplona.logicode.activities.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Actividad calificable de un curso (RF-10, RF-11): enunciado, formas de entrega, porcentaje, apertura, cierre e
 * intentos. No se borra: se archiva.
 */
@Entity
@Table(name = "activity")
public class Activity {

    /** Datos editables de una actividad. {@code fileGroups} solo aplica si se aceptan archivos. */
    public record Definition(String title, ActivityKind kind, String statement, boolean acceptsText,
            boolean acceptsFiles, boolean acceptsPhotos, Set<FileGroup> fileGroups, BigDecimal weightPercent,
            Instant opensAt, Instant closesAt, int maxAttempts) {
    }

    public static final int MAX_ATTEMPTS = 10;

    @Id
    private UUID id;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityKind kind;

    @Column(nullable = false)
    private String statement;

    @Column(name = "accepts_text", nullable = false)
    private boolean acceptsText;

    @Column(name = "accepts_files", nullable = false)
    private boolean acceptsFiles;

    @Column(name = "accepts_photos", nullable = false)
    private boolean acceptsPhotos;

    @Convert(converter = FileGroupsConverter.class)
    @Column(name = "accepted_files")
    private Set<FileGroup> fileGroups = EnumSet.noneOf(FileGroup.class);

    @Column(name = "weight_percent")
    private BigDecimal weightPercent;

    @Column(name = "opens_at", nullable = false)
    private Instant opensAt;

    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityStatus status;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Activity() {
    }

    public Activity(UUID courseId, Definition definition, UUID createdBy, Instant now) {
        this.id = UUID.randomUUID();
        this.courseId = courseId;
        this.status = ActivityStatus.DRAFT;
        this.createdBy = createdBy;
        this.createdAt = now;
        apply(definition, now);
    }

    public void update(Definition definition, Instant now) {
        apply(definition, now);
    }

    public void publish(Instant now) {
        this.status = ActivityStatus.PUBLISHED;
        this.updatedAt = now;
    }

    public void archive(Instant now) {
        this.archivedAt = now;
        this.updatedAt = now;
    }

    /** Fecha límite para un estudiante: el cierre o la ampliación que le dio el docente, la que sea mayor. */
    public Instant deadlineWith(DeadlineException exception) {
        if (exception == null || exception.getExtendedUntil() == null
                || exception.getExtendedUntil().isBefore(closesAt)) {
            return closesAt;
        }
        return exception.getExtendedUntil();
    }

    public int attemptsWith(DeadlineException exception) {
        return maxAttempts + (exception == null ? 0 : exception.getExtraAttempts());
    }

    public boolean isOpenAt(Instant now) {
        return !now.isBefore(opensAt);
    }

    public boolean isClosedAt(Instant now) {
        return now.isAfter(closesAt);
    }

    public boolean isPublished() {
        return status == ActivityStatus.PUBLISHED;
    }

    public boolean isQuiz() {
        return kind.isQuiz();
    }

    public boolean isPractice() {
        return kind == ActivityKind.PRACTICE_QUIZ;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    private void apply(Definition d, Instant now) {
        if (!d.closesAt().isAfter(d.opensAt())) {
            throw new IllegalArgumentException("El cierre debe ser posterior a la apertura");
        }
        if (!d.kind().isQuiz() && !(d.acceptsText() || d.acceptsFiles() || d.acceptsPhotos())) {
            throw new IllegalArgumentException("La actividad debe aceptar alguna forma de entrega");
        }
        if (d.kind() == ActivityKind.PRACTICE_QUIZ && d.weightPercent() != null) {
            throw new IllegalArgumentException("Un quiz de repaso no tiene porcentaje");
        }
        this.title = d.title();
        this.kind = d.kind();
        this.statement = d.statement();
        this.acceptsText = d.acceptsText();
        this.acceptsFiles = d.acceptsFiles();
        this.acceptsPhotos = d.acceptsPhotos();
        this.fileGroups = d.acceptsFiles() ? EnumSet.copyOf(d.fileGroups()) : EnumSet.noneOf(FileGroup.class);
        this.weightPercent = d.weightPercent();
        this.opensAt = d.opensAt();
        this.closesAt = d.closesAt();
        this.maxAttempts = d.maxAttempts();
        this.updatedAt = now;
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

    public ActivityKind getKind() {
        return kind;
    }

    public String getStatement() {
        return statement;
    }

    public boolean isAcceptsText() {
        return acceptsText;
    }

    public boolean isAcceptsFiles() {
        return acceptsFiles;
    }

    public boolean isAcceptsPhotos() {
        return acceptsPhotos;
    }

    public Set<FileGroup> getFileGroups() {
        return fileGroups.isEmpty() ? EnumSet.noneOf(FileGroup.class) : EnumSet.copyOf(fileGroups);
    }

    public BigDecimal getWeightPercent() {
        return weightPercent;
    }

    public Instant getOpensAt() {
        return opensAt;
    }

    public Instant getClosesAt() {
        return closesAt;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public ActivityStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
