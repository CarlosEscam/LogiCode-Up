package co.edu.unipamplona.logicode.grading.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.application.GradeRepository;
import co.edu.unipamplona.logicode.grading.domain.Grade;
import org.springframework.stereotype.Repository;

@Repository
class GradeRepositoryAdapter implements GradeRepository {

    private final JpaGradeRepository jpa;

    GradeRepositoryAdapter(JpaGradeRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Grade save(Grade grade) {
        return jpa.save(grade);
    }

    @Override
    public Optional<Grade> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Grade> findBySubmission(UUID submissionId) {
        return jpa.findBySubmissionId(submissionId);
    }

    @Override
    public List<Grade> findBySubmissions(Collection<UUID> submissionIds) {
        return submissionIds.isEmpty() ? List.of() : jpa.findBySubmissionIdIn(submissionIds);
    }
}
