package co.edu.unipamplona.logicode.grading.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Criterio de una rúbrica con su puntaje máximo. */
@Embeddable
public class RubricCriterion {

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "max_points", nullable = false)
    private BigDecimal maxPoints;

    protected RubricCriterion() {
    }

    public RubricCriterion(String name, String description, BigDecimal maxPoints) {
        if (maxPoints == null || maxPoints.signum() <= 0) {
            throw new IllegalArgumentException("El puntaje del criterio debe ser positivo");
        }
        this.name = name;
        this.description = description;
        this.maxPoints = maxPoints;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getMaxPoints() {
        return maxPoints;
    }
}
