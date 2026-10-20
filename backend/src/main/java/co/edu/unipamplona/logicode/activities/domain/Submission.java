package co.edu.unipamplona.logicode.activities.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/** Un intento de entrega de un estudiante (RF-13). No cambia después de enviado. */
@Entity
@Table(name = "submission")
public class Submission {

    public static final int MAX_FILES = 5;

    @Id
    private UUID id;

    @Column(name = "activity_id", nullable = false, updatable = false)
    private UUID activityId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(nullable = false, updatable = false)
    private int attempt;

    @Column(name = "text_answer", updatable = false)
    private String textAnswer;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "submission_file", joinColumns = @JoinColumn(name = "submission_id"))
    @OrderColumn(name = "position")
    @Column(name = "file_id", nullable = false)
    private List<UUID> fileIds = new ArrayList<>();

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(nullable = false, updatable = false)
    private boolean late;

    protected Submission() {
    }

    public Submission(UUID activityId, UUID studentId, int attempt, String textAnswer, List<UUID> fileIds,
            boolean late, Instant now) {
        if (textAnswer == null && fileIds.isEmpty()) {
            throw new IllegalArgumentException("La entrega no tiene contenido");
        }
        if (fileIds.size() > MAX_FILES) {
            throw new IllegalArgumentException("Demasiados archivos");
        }
        this.id = UUID.randomUUID();
        this.activityId = activityId;
        this.studentId = studentId;
        this.attempt = attempt;
        this.textAnswer = textAnswer;
        this.fileIds = new ArrayList<>(fileIds);
        this.late = late;
        this.submittedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getActivityId() {
        return activityId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public int getAttempt() {
        return attempt;
    }

    public String getTextAnswer() {
        return textAnswer;
    }

    public List<UUID> getFileIds() {
        return List.copyOf(fileIds);
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public boolean isLate() {
        return late;
    }
}
