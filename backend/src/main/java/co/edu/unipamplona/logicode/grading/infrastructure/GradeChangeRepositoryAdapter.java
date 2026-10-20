package co.edu.unipamplona.logicode.grading.infrastructure;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.application.GradeChangeRepository;
import co.edu.unipamplona.logicode.grading.domain.GradeChange;
import org.springframework.stereotype.Repository;

@Repository
class GradeChangeRepositoryAdapter implements GradeChangeRepository {

    private final JpaGradeChangeRepository jpa;

    GradeChangeRepositoryAdapter(JpaGradeChangeRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public GradeChange save(GradeChange change) {
        return jpa.save(change);
    }

    @Override
    public List<GradeChange> findByGrade(UUID gradeId) {
        return jpa.findByGradeIdOrderByChangedAtAsc(gradeId);
    }
}
