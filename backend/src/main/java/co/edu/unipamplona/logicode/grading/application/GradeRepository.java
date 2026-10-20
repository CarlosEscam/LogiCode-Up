package co.edu.unipamplona.logicode.grading.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.Grade;

public interface GradeRepository {

    Grade save(Grade grade);

    Optional<Grade> findBySubmission(UUID submissionId);

    List<Grade> findBySubmissions(Collection<UUID> submissionIds);
}
