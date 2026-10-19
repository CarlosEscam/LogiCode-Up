package co.edu.unipamplona.logicode.courses.application;

import java.time.Instant;
import java.util.UUID;

/** Curso tal como lo ve un actor, con lo que ese actor puede hacer en él. */
public record CourseView(UUID id, String name, String period, String description, String ownerName,
        long enrolledCount, boolean canManageContent, boolean canManageEnrollments, Instant createdAt) {
}
