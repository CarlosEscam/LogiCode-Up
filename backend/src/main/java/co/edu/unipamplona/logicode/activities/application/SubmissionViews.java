package co.edu.unipamplona.logicode.activities.application;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.domain.Activity;
import co.edu.unipamplona.logicode.activities.domain.Submission;
import co.edu.unipamplona.logicode.files.application.FileStore;
import co.edu.unipamplona.logicode.files.application.StoredFileInfo;
import co.edu.unipamplona.logicode.identity.application.UserDirectory;
import org.springframework.stereotype.Component;

@Component
class SubmissionViews {

    private final FileStore fileStore;
    private final UserDirectory directory;

    SubmissionViews(FileStore fileStore, UserDirectory directory) {
        this.fileStore = fileStore;
        this.directory = directory;
    }

    SubmissionView view(Submission submission, Activity activity) {
        Map<UUID, StoredFileInfo> files = fileStore.describe(submission.getFileIds());
        List<StoredFileInfo> ordered = submission.getFileIds().stream().map(files::get).filter(Objects::nonNull)
                .toList();
        return new SubmissionView(submission.getId(), activity.getId(), activity.getTitle(), activity.getCourseId(),
                submission.getStudentId(), directory.fullNameOf(submission.getStudentId()).orElse(null),
                submission.getAttempt(), submission.getTextAnswer(), ordered, submission.getSubmittedAt(),
                submission.isLate());
    }
}
