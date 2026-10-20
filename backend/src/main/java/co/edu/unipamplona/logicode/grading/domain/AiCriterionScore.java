package co.edu.unipamplona.logicode.grading.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Puntos que la IA asignó a un criterio de la rúbrica, con su comentario. */
@Embeddable
public class AiCriterionScore {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal points;

    @Column(name = "max_points", nullable = false)
    private BigDecimal maxPoints;

    private String comment;

    protected AiCriterionScore() {
    }

    public AiCriterionScore(String name, BigDecimal points, BigDecimal maxPoints, String comment) {
        this.name = name;
        this.points = points;
        this.maxPoints = maxPoints;
        this.comment = comment;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPoints() {
        return points;
    }

    public BigDecimal getMaxPoints() {
        return maxPoints;
    }

    public String getComment() {
        return comment;
    }
}
