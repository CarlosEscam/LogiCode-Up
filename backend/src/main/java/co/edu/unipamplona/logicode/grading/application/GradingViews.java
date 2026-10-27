package co.edu.unipamplona.logicode.grading.application;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.edu.unipamplona.logicode.grading.domain.Grade;
import co.edu.unipamplona.logicode.grading.domain.RubricVersion;
import co.edu.unipamplona.logicode.identity.application.UserDirectory;
import org.springframework.stereotype.Component;

@Component
class GradingViews {

    private final RubricRepository rubrics;
    private final UserDirectory directory;

    GradingViews(RubricRepository rubrics, UserDirectory directory) {
        this.rubrics = rubrics;
        this.directory = directory;
    }

    static RubricView rubric(RubricVersion r, boolean withExpectedAnswer) {
        return new RubricView(r.getId(), r.getActivityId(), r.getNumber(), r.getStatement(),
                withExpectedAnswer ? r.getExpectedAnswer() : null,
                r.getCriteria().stream().map(c -> new RubricView.Criterion(c.getName(), c.getDescription(),
                        c.getMaxPoints())).toList(),
                r.totalPoints(), withExpectedAnswer ? r.getTestCases().stream().map(t -> new RubricView.TestCase(
                        t.getInput(), t.getExpectedOutput())).toList() : null,
                r.getTestCases().size(), r.getCreatedAt());
    }

    /** Vistas de las notas; con {@code ownerOnly} se ocultan nota y retroalimentación aún no publicadas. */
    Map<UUID, GradeView> grades(Collection<Grade> grades, boolean ownerOnly) {
        if (grades.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Integer> versions = rubrics.findAllById(grades.stream().map(Grade::getRubricVersionId)
                .distinct().toList()).stream()
                .collect(Collectors.toMap(RubricVersion::getId, RubricVersion::getNumber));
        Map<UUID, String> names = directory.fullNamesOf(grades.stream().map(Grade::getGradedBy)
                .filter(Objects::nonNull).distinct().toList());
        return grades.stream().map(g -> {
            boolean hidden = ownerOnly && !g.isPublished();
            return new GradeView(g.getId(), g.getSubmissionId(), g.getStatus(),
                    ownerOnly ? null : g.getReviewReason(), hidden ? null : g.getScore(),
                    hidden ? null : g.getFeedback(), hidden ? null : g.getSource(),
                    hidden ? null : names.get(g.getGradedBy()), hidden ? null : g.getGradedAt(),
                    versions.getOrDefault(g.getRubricVersionId(), 0));
        }).collect(Collectors.toMap(GradeView::submissionId, Function.identity()));
    }

    GradeView grade(Grade grade, boolean ownerOnly) {
        return grades(List.of(grade), ownerOnly).get(grade.getSubmissionId());
    }

    Map<UUID, String> names(Collection<UUID> userIds) {
        return directory.fullNamesOf(userIds);
    }
}
