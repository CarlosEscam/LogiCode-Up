package co.edu.unipamplona.logicode.activities.infrastructure;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import co.edu.unipamplona.logicode.files.application.FileUsage;
import org.springframework.stereotype.Component;

/** Los archivos de las entregas se conservan siempre: son la evidencia de la nota. */
@Component
class SubmissionFileUsage implements FileUsage {

    private final JpaSubmissionRepository submissions;

    SubmissionFileUsage(JpaSubmissionRepository submissions) {
        this.submissions = submissions;
    }

    @Override
    public Set<UUID> inUse(Collection<UUID> fileIds) {
        return submissions.fileIdsInUse(fileIds);
    }
}
