package co.edu.unipamplona.logicode.grading.infrastructure;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.GradeChange;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaGradeChangeRepository extends JpaRepository<GradeChange, UUID> {

    List<GradeChange> findByGradeIdOrderByChangedAtAsc(UUID gradeId);
}
