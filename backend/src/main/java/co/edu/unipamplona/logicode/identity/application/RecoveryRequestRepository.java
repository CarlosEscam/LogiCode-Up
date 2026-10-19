package co.edu.unipamplona.logicode.identity.application;

import java.time.LocalDate;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.RecoveryKind;
import co.edu.unipamplona.logicode.identity.domain.RecoveryRequest;

public interface RecoveryRequestRepository {

    RecoveryRequest save(RecoveryRequest request);

    /** Solicitudes que cuentan para el límite diario (todas excepto las rechazadas por límite). */
    long countCountedFor(UUID userId, RecoveryKind kind, LocalDate businessDate);
}
