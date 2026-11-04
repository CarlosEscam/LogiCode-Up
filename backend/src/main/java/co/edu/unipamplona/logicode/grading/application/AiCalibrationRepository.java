package co.edu.unipamplona.logicode.grading.application;

import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.AiCalibration;

public interface AiCalibrationRepository {

    AiCalibration save(AiCalibration calibration);

    Optional<AiCalibration> findByCourse(UUID courseId);
}
