package co.edu.unipamplona.logicode.grading.domain;

import java.math.BigDecimal;
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

/**
 * RN-20: rúbrica de una actividad en un momento dado (enunciado, respuesta esperada y criterios). No cambia: editar la
 * rúbrica o el enunciado crea una versión nueva, y cada entrega queda ligada a la vigente cuando se envió.
 */
@Entity
@Table(name = "rubric_version")
public class RubricVersion {

    public static final int MAX_CRITERIA = 20;
    public static final int MAX_TEST_CASES = 20;

    @Id
    private UUID id;

    @Column(name = "activity_id", nullable = false, updatable = false)
    private UUID activityId;

    @Column(nullable = false, updatable = false)
    private int number;

    @Column(nullable = false, updatable = false)
    private String statement;

    @Column(name = "expected_answer", updatable = false)
    private String expectedAnswer;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rubric_criterion", joinColumns = @JoinColumn(name = "rubric_version_id"))
    @OrderColumn(name = "position")
    private List<RubricCriterion> criteria = new ArrayList<>();

    /** RF-16: con casos de prueba, las entregas con código PSeInt se califican ejecutándolo. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rubric_test_case", joinColumns = @JoinColumn(name = "rubric_version_id"))
    @OrderColumn(name = "position")
    private List<RubricTestCase> testCases = new ArrayList<>();

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RubricVersion() {
    }

    public RubricVersion(UUID activityId, int number, String statement, String expectedAnswer,
            List<RubricCriterion> criteria, List<RubricTestCase> testCases, UUID createdBy, Instant now) {
        if (criteria.isEmpty() || criteria.size() > MAX_CRITERIA) {
            throw new IllegalArgumentException("La rúbrica necesita entre 1 y " + MAX_CRITERIA + " criterios");
        }
        if (testCases.size() > MAX_TEST_CASES) {
            throw new IllegalArgumentException("La rúbrica admite hasta " + MAX_TEST_CASES + " casos de prueba");
        }
        this.id = UUID.randomUUID();
        this.activityId = activityId;
        this.number = number;
        this.statement = statement;
        this.expectedAnswer = expectedAnswer;
        this.criteria = new ArrayList<>(criteria);
        this.testCases = new ArrayList<>(testCases);
        this.createdBy = createdBy;
        this.createdAt = now;
    }

    /** Versión siguiente con otro enunciado y los mismos criterios. */
    public RubricVersion withStatement(String newStatement, UUID changedBy, Instant now) {
        return new RubricVersion(activityId, number + 1, newStatement, expectedAnswer, criteria, testCases, changedBy,
                now);
    }

    public BigDecimal totalPoints() {
        return criteria.stream().map(RubricCriterion::getMaxPoints).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public UUID getId() {
        return id;
    }

    public UUID getActivityId() {
        return activityId;
    }

    public int getNumber() {
        return number;
    }

    public String getStatement() {
        return statement;
    }

    public String getExpectedAnswer() {
        return expectedAnswer;
    }

    public List<RubricCriterion> getCriteria() {
        return List.copyOf(criteria);
    }

    public List<RubricTestCase> getTestCases() {
        return List.copyOf(testCases);
    }

    public boolean hasTestCases() {
        return !testCases.isEmpty();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
