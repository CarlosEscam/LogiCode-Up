package co.edu.unipamplona.logicode.identity.application;

import java.time.Instant;

import co.edu.unipamplona.logicode.identity.domain.AuditAction;
import co.edu.unipamplona.logicode.identity.domain.AuditEvent;
import co.edu.unipamplona.logicode.shared.application.PageResult;

public interface AuditEventRepository {

    AuditEvent save(AuditEvent event);

    long countByActionAndSubjectAndOutcomeSince(AuditAction action, String subject, AuditEvent.Outcome outcome,
            Instant since);

    /** Igual, pero solo los eventos con este {@code details} (p. ej. fallos desde un equipo conocido). */
    long countByActionAndSubjectAndOutcomeAndDetailsSince(AuditAction action, String subject,
            AuditEvent.Outcome outcome, String details, Instant since);

    /** Bloqueo de PostgreSQL hasta el fin de la transacción: serializa los intentos sobre un mismo sujeto (P-05). */
    void lockSubject(String subject);

    PageResult<AuditEvent> listLatest(int page, int size);
}
