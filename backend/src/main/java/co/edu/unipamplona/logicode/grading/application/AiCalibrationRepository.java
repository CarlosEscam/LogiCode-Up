package co.edu.unipamplona.logicode.grading.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.domain.AiCalibration;

public interface AiCalibrationRepository {

    AiCalibration save(AiCalibration calibration);

    Optional<AiCalibration> findByCourse(UUID courseId);

    /**
     * Crea la fila del curso si falta y la bloquea hasta el fin de la transacción, para que dos cierres a la vez se
     * atiendan uno tras otro (defecto D-1 de la II-04).
     */
    AiCalibration lockForChange(UUID courseId, Instant now);
}
