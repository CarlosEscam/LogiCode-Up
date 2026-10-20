package co.edu.unipamplona.logicode.grading.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaGradeRepository extends JpaRepository<Grade, UUID> {

    Optional<Grade> findBySubmissionId(UUID submissionId);

    List<Grade> findBySubmissionIdIn(Collection<UUID> submissionIds);
}
