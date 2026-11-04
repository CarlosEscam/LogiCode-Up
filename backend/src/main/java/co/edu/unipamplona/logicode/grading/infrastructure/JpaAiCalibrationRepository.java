package co.edu.unipamplona.logicode.grading.infrastructure;

import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.AiCalibration;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaAiCalibrationRepository extends JpaRepository<AiCalibration, UUID> {
}
