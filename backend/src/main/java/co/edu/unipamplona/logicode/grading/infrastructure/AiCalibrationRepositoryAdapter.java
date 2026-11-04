package co.edu.unipamplona.logicode.grading.infrastructure;

import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.application.AiCalibrationRepository;
import co.edu.unipamplona.logicode.grading.domain.AiCalibration;
import org.springframework.stereotype.Repository;

@Repository
class AiCalibrationRepositoryAdapter implements AiCalibrationRepository {

    private final JpaAiCalibrationRepository jpa;

    AiCalibrationRepositoryAdapter(JpaAiCalibrationRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AiCalibration save(AiCalibration calibration) {
        return jpa.save(calibration);
    }

    @Override
    public Optional<AiCalibration> findByCourse(UUID courseId) {
        return jpa.findById(courseId);
    }
}
