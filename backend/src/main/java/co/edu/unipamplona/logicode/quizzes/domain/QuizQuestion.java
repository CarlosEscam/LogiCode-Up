package co.edu.unipamplona.logicode.quizzes.domain;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

/** Pregunta del banco de un curso con su clave de respuesta. No se borra: se archiva. */
@Entity
@Table(name = "quiz_question")
public class QuizQuestion {

    public record Definition(QuestionKind kind, String prompt, int points, String explanation,
            List<QuizOption> options, List<String> acceptedAnswers) {
    }

    public static final int MAX_OPTIONS = 6;
    public static final int MAX_ACCEPTED_ANSWERS = 5;
    public static final int MAX_POINTS = 10;

    @Id
    private UUID id;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionKind kind;

    @Column(nullable = false)
    private String prompt;

    @Column(nullable = false)
    private int points;

    private String explanation;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quiz_question_option", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "position")
    private List<QuizOption> options = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quiz_question_answer", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "position")
    @Column(name = "answer", nullable = false)
    private List<String> acceptedAnswers = new ArrayList<>();

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

    protected QuizQuestion() {
    }

    public QuizQuestion(UUID courseId, Definition definition, UUID createdBy, Instant now) {
        this.id = UUID.randomUUID();
        this.courseId = courseId;
        this.createdBy = createdBy;
        this.createdAt = now;
        apply(definition, now);
    }

    public void update(Definition definition, Instant now) {
        apply(definition, now);
    }

    public void archive(Instant now) {
        this.archivedAt = now;
        this.updatedAt = now;
    }

    /** Acierto de una respuesta. Sin respuesta, no acierta. */
    public boolean isCorrect(Set<Integer> choices, String text) {
        if (kind == QuestionKind.SHORT) {
            String given = normalize(text);
            return !given.isEmpty() && acceptedAnswers.stream().map(QuizQuestion::normalize).anyMatch(given::equals);
        }
        return !choices.isEmpty() && choices.equals(correctChoices());
    }

    public Set<Integer> correctChoices() {
        return IntStream.range(0, options.size()).filter(i -> options.get(i).isCorrect()).boxed()
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** Sin mayúsculas, tildes ni espacios repetidos: «Mientras» y «  mientras » son la misma respuesta. */
    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").strip()
                .replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private void apply(Definition d, Instant now) {
        if (d.points() < 1 || d.points() > MAX_POINTS) {
            throw new IllegalArgumentException("Los puntos van de 1 a " + MAX_POINTS);
        }
        if (d.kind().hasOptions()) {
            long correct = d.options().stream().filter(QuizOption::isCorrect).count();
            boolean single = d.kind() != QuestionKind.MULTIPLE;
            if (d.options().size() < 2 || d.options().size() > MAX_OPTIONS || correct < 1 || single && correct != 1
                    || d.kind() == QuestionKind.TRUE_FALSE && d.options().size() != 2) {
                throw new IllegalArgumentException("Opciones no válidas para " + d.kind());
            }
        } else if (d.acceptedAnswers().isEmpty() || d.acceptedAnswers().size() > MAX_ACCEPTED_ANSWERS) {
            throw new IllegalArgumentException("Respuestas aceptadas no válidas");
        }
        this.kind = d.kind();
        this.prompt = d.prompt();
        this.points = d.points();
        this.explanation = d.explanation();
        this.options = d.kind().hasOptions() ? new ArrayList<>(d.options()) : new ArrayList<>();
        this.acceptedAnswers = d.kind().hasOptions() ? new ArrayList<>() : new ArrayList<>(d.acceptedAnswers());
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public QuestionKind getKind() {
        return kind;
    }

    public String getPrompt() {
        return prompt;
    }

    public int getPoints() {
        return points;
    }

    public String getExplanation() {
        return explanation;
    }

    public List<QuizOption> getOptions() {
        return List.copyOf(options);
    }

    public List<String> getAcceptedAnswers() {
        return List.copyOf(acceptedAnswers);
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
