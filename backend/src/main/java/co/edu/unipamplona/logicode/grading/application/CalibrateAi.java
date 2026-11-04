package co.edu.unipamplona.logicode.grading.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.ActivityCatalog;
import co.edu.unipamplona.logicode.grading.domain.AiCalibration;
import co.edu.unipamplona.logicode.grading.domain.AiOutcome;
import co.edu.unipamplona.logicode.grading.domain.CalibrationStats;
import co.edu.unipamplona.logicode.grading.domain.Grade;
import co.edu.unipamplona.logicode.grading.domain.GradeSource;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calibración de la IA por curso (T-90): estado, concordancia con el docente, cierre y reapertura. La consulta el
 * docente del curso y las cuentas Administrador; la cambia quien puede calificar en el curso.
 */
@Service
public class CalibrateAi {

    /** Estado para la pantalla del curso. */
    public record View(boolean calibrating, int minimum, int compared, int agreeing, int wouldHaveFailed,
            Integer concordancePercent, Instant closedAt, boolean canChange) {
    }

    private final AiCalibrationRepository calibrations;
    private final GradeRepository grades;
    private final AiEvaluationRepository evaluations;
    private final ActivityCatalog catalog;
    private final GradingAccess access;
    private final AiGradingSettings settings;
    private final Clock clock;

    public CalibrateAi(AiCalibrationRepository calibrations, GradeRepository grades,
            AiEvaluationRepository evaluations, ActivityCatalog catalog, GradingAccess access,
            AiGradingSettings settings, Clock clock) {
        this.calibrations = calibrations;
        this.grades = grades;
        this.evaluations = evaluations;
        this.catalog = catalog;
        this.access = access;
        this.settings = settings;
        this.clock = clock;
    }

    /** Un curso sin registro está en calibración. */
    public boolean isCalibrating(UUID courseId) {
        return calibrations.findByCourse(courseId).map(AiCalibration::isOpen).orElse(true);
    }

    @Transactional(readOnly = true)
    public View status(Actor actor, UUID courseId) {
        access.requireViewCourseWork(actor, courseId);
        return view(actor, courseId, calibrations.findByCourse(courseId).orElse(null), stats(courseId));
    }

    @Transactional
    public View close(Actor actor, UUID courseId) {
        requireChange(actor, courseId);
        CalibrationStats stats = stats(courseId);
        AiCalibration calibration = calibrations.findByCourse(courseId)
                .orElseGet(() -> AiCalibration.open(courseId, clock.instant()));
        if (!calibration.isOpen()) {
            throw ApplicationException.conflict("La calibración de este curso ya está cerrada.");
        }
        try {
            calibration.close(actor.userId(), stats, settings.calibrationMinComparisons(), clock.instant());
        } catch (IllegalArgumentException e) {
            throw ApplicationException.businessRule(e.getMessage());
        }
        return view(actor, courseId, calibrations.save(calibration), stats);
    }

    @Transactional
    public View reopen(Actor actor, UUID courseId) {
        requireChange(actor, courseId);
        AiCalibration calibration = calibrations.findByCourse(courseId).filter(c -> !c.isOpen())
                .orElseThrow(() -> ApplicationException.conflict("La calibración de este curso ya está abierta."));
        calibration.reopen(clock.instant());
        return view(actor, courseId, calibrations.save(calibration), stats(courseId));
    }

    private void requireChange(Actor actor, UUID courseId) {
        access.requireViewCourseWork(actor, courseId);
        if (!access.canGrade(actor, courseId)) {
            throw ApplicationException.forbidden("Solo el docente del curso puede cambiar la calibración de la IA.");
        }
    }

    // shortcut: una consulta de evaluaciones por nota publicada; basta para un curso de primer semestre, pasar a una
    // consulta única si un curso supera unos miles de entregas.
    private CalibrationStats stats(UUID courseId) {
        List<UUID> submissionIds = catalog.activitiesOf(courseId).stream()
                .flatMap(a -> catalog.submissionIdsOf(a.id()).stream()).toList();
        List<CalibrationStats.Pair> pairs = grades.findBySubmissions(submissionIds).stream()
                .filter(g -> g.isPublished() && g.getSource() == GradeSource.TEACHER)
                .map(this::pairFor).filter(Objects::nonNull).toList();
        return CalibrationStats.of(pairs);
    }

    /** La última propuesta válida de la IA, si la habría publicado sola, frente a la nota del docente. */
    private CalibrationStats.Pair pairFor(Grade grade) {
        return evaluations.findByGrade(grade.getId()).stream()
                .filter(e -> e.getOutcome() != AiOutcome.FAILED).findFirst()
                .filter(e -> e.getOutcome() == AiOutcome.PUBLISHED && e.getProposedScore() != null)
                .map(e -> new CalibrationStats.Pair(e.getProposedScore(), grade.getScore())).orElse(null);
    }

    private View view(Actor actor, UUID courseId, AiCalibration calibration, CalibrationStats stats) {
        boolean open = calibration == null || calibration.isOpen();
        return new View(open, settings.calibrationMinComparisons(), stats.compared(), stats.agreeing(),
                stats.wouldHaveFailed(), stats.concordancePercent(), open ? null : calibration.getClosedAt(),
                access.canGrade(actor, courseId));
    }
}
