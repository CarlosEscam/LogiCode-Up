package co.edu.unipamplona.logicode.grading.application;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.GradeChange;

public interface GradeChangeRepository {

    GradeChange save(GradeChange change);

    /** Cambios de la nota, del más antiguo al más reciente. */
    List<GradeChange> findByGrade(UUID gradeId);
}
