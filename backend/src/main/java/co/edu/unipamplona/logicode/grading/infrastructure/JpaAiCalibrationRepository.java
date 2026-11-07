package co.edu.unipamplona.logicode.grading.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.AiCalibration;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface JpaAiCalibrationRepository extends JpaRepository<AiCalibration, UUID> {

    @Modifying
    @Query(value = "INSERT INTO ai_calibration (course_id, updated_at, version) VALUES (:courseId, :now, 0)"
            + " ON CONFLICT (course_id) DO NOTHING", nativeQuery = true)
    int insertIfMissing(UUID courseId, Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AiCalibration c where c.courseId = :courseId")
    Optional<AiCalibration> lockByCourse(UUID courseId);
}
