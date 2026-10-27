package co.edu.unipamplona.logicode.quizzes.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Intento de un quiz (RN-13). Vence al agotarse el tiempo total o al cerrar el quiz, lo que pase primero; entonces se
 * envía con las respuestas guardadas. La nota depende solo de los aciertos (RN-14): 5 × puntos ÷ puntos posibles.
 */
@Entity
@Table(name = "quiz_attempt")
public class QuizAttempt {

    /** Margen para la respuesta que el navegador envía justo al agotarse el tiempo. */
    public static final Duration GRACE = Duration.ofSeconds(30);

    @Id
    private UUID id;

    @Column(name = "activity_id", nullable = false, updatable = false)
    private UUID activityId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(nullable = false, updatable = false)
    private int number;

    @Column(nullable = false, updatable = false)
    private boolean practice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(nullable = false, updatable = false)
    private Instant deadline;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "question_count", nullable = false, updatable = false)
    private int questionCount;

    private Integer points;

    @Column(name = "max_points", nullable = false, updatable = false)
    private int maxPoints;

    private BigDecimal score;

    @Column(name = "submission_id")
    private UUID submissionId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quiz_attempt_answer", joinColumns = @JoinColumn(name = "attempt_id"))
    @OrderColumn(name = "position")
    private List<QuizAnswer> answers = new ArrayList<>();

    @Version
    private long version;

    protected QuizAttempt() {
    }

    public QuizAttempt(UUID activityId, UUID studentId, int number, boolean practice, List<QuizQuestion> questions,
            Instant now, Instant deadline) {
        this.id = UUID.randomUUID();
        this.activityId = activityId;
        this.studentId = studentId;
        this.number = number;
        this.practice = practice;
        this.status = AttemptStatus.IN_PROGRESS;
        this.startedAt = now;
        this.deadline = deadline;
        this.questionCount = questions.size();
        this.maxPoints = questions.stream().mapToInt(QuizQuestion::getPoints).sum();
    }

    public boolean isFinished() {
        return status == AttemptStatus.FINISHED;
    }

    /** Pasó el tiempo y el margen sin que se enviara: hay que enviarlo con lo guardado. */
    public boolean isExpiredAt(Instant now) {
        return !isFinished() && now.isAfter(deadline.plus(GRACE));
    }

    public void saveAnswers(List<QuizAnswer> newAnswers, Instant now) {
        if (isFinished() || isExpiredAt(now)) {
            throw new IllegalStateException("El intento ya no recibe respuestas");
        }
        this.answers = new ArrayList<>(newAnswers);
    }

    /** Califica con las preguntas del quiz, en su orden, y cierra el intento. */
    public void finish(List<QuizQuestion> questions, Instant now) {
        if (isFinished()) {
            throw new IllegalStateException("El intento ya terminó");
        }
        Map<UUID, QuizAnswer> byQuestion = answers.stream()
                .collect(Collectors.toMap(QuizAnswer::getQuestionId, Function.identity(), (a, b) -> b));
        List<QuizAnswer> graded = new ArrayList<>();
        int correct = 0;
        int earned = 0;
        for (QuizQuestion question : questions) {
            QuizAnswer answer = byQuestion.getOrDefault(question.getId(), new QuizAnswer(question.getId(), null, null));
            boolean right = question.isCorrect(answer.choiceSet(), answer.getTextAnswer());
            answer.grade(right);
            graded.add(answer);
            if (right) {
                correct++;
                earned += question.getPoints();
            }
        }
        this.answers = graded;
        this.correctCount = correct;
        this.points = earned;
        this.score = maxPoints == 0 ? BigDecimal.ZERO.setScale(1)
                : BigDecimal.valueOf(5L * earned).divide(BigDecimal.valueOf(maxPoints), 1, RoundingMode.HALF_UP);
        this.finishedAt = now.isAfter(deadline) ? deadline : now;
        this.status = AttemptStatus.FINISHED;
    }

    public void linkSubmission(UUID submissionId) {
        this.submissionId = submissionId;
    }

    /** Tiempo usado, para el ranking (RN-14). */
    public long durationSeconds() {
        return finishedAt == null ? 0 : Math.max(0, Duration.between(startedAt, finishedAt).toSeconds());
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

    public int getNumber() {
        return number;
    }

    public boolean isPractice() {
        return practice;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public Integer getPoints() {
        return points;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public BigDecimal getScore() {
        return score;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public List<QuizAnswer> getAnswers() {
        return List.copyOf(answers);
    }
}
