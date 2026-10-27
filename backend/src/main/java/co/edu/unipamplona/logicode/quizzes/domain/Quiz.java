package co.edu.unipamplona.logicode.quizzes.domain;

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
import jakarta.persistence.Version;

/** RN-13: preguntas (del banco del curso, en orden) y tiempo total de un quiz. */
@Entity
@Table(name = "quiz")
public class Quiz {

    public static final int MAX_QUESTIONS = 50;
    public static final int MAX_MINUTES = 300;

    @Id
    @Column(name = "activity_id")
    private UUID activityId;

    @Column(name = "time_limit_minutes", nullable = false)
    private int timeLimitMinutes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quiz_item", joinColumns = @JoinColumn(name = "quiz_id"))
    @OrderColumn(name = "position")
    @Column(name = "question_id", nullable = false)
    private List<UUID> questionIds = new ArrayList<>();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Quiz() {
    }

    public Quiz(UUID activityId, int timeLimitMinutes, List<UUID> questionIds, Instant now) {
        this.activityId = activityId;
        define(timeLimitMinutes, questionIds, now);
    }

    public void define(int timeLimitMinutes, List<UUID> questionIds, Instant now) {
        if (timeLimitMinutes < 1 || timeLimitMinutes > MAX_MINUTES) {
            throw new IllegalArgumentException("Tiempo fuera de rango");
        }
        if (questionIds.isEmpty() || questionIds.size() > MAX_QUESTIONS
                || questionIds.stream().distinct().count() != questionIds.size()) {
            throw new IllegalArgumentException("Preguntas no válidas");
        }
        this.timeLimitMinutes = timeLimitMinutes;
        this.questionIds = new ArrayList<>(questionIds);
        this.updatedAt = now;
    }

    public UUID getActivityId() {
        return activityId;
    }

    public int getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public List<UUID> getQuestionIds() {
        return List.copyOf(questionIds);
    }
}
