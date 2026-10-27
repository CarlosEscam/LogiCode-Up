package co.edu.unipamplona.logicode.grading.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import co.edu.unipamplona.logicode.activities.application.ActivityCatalog;
import co.edu.unipamplona.logicode.activities.application.ActivityView;
import co.edu.unipamplona.logicode.activities.application.BrowseSubmissions;
import co.edu.unipamplona.logicode.courses.application.CourseAccess;
import co.edu.unipamplona.logicode.courses.application.CourseRoster;
import co.edu.unipamplona.logicode.grading.domain.Grade;
import co.edu.unipamplona.logicode.grading.domain.GradebookRules;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Planilla del curso (RF-18, RF-20, RN-07): una fila por inscrito y una columna por actividad calificable, con el
 * avance parcial separado de la nota definitiva y estadísticas básicas por actividad. El docente del curso y las
 * cuentas Administrador ven todo; un estudiante solo su fila y sin estadísticas del grupo.
 */
@Service
public class Gradebook {

    /**
     * {@code GRADED}: nota publicada del último intento. {@code OVERDUE}: venció sin entrega (0,0, RN-05).
     * {@code IN_REVIEW}: entregada, sin nota publicada. {@code PENDING}: aún puede entregar o no ha abierto.
     */
    public enum CellStatus {
        GRADED,
        OVERDUE,
        IN_REVIEW,
        PENDING
    }

    public record Cell(UUID activityId, CellStatus status, BigDecimal score, UUID submissionId) {

        /** Resultado que cuenta en la planilla: la nota publicada o 0,0 por vencida. */
        BigDecimal result() {
            return switch (status) {
                case GRADED -> score;
                case OVERDUE -> BigDecimal.ZERO.setScale(1);
                default -> null;
            };
        }
    }

    public record Row(String cedula, String fullName, boolean registered, List<Cell> cells, BigDecimal partial,
            BigDecimal definitive) {
    }

    /** Promedio, mínimo y máximo se calculan sobre los resultados (notas publicadas y vencidas en 0,0). */
    public record ColumnStats(int submitted, int graded, int inReview, int overdue, BigDecimal average,
            BigDecimal min, BigDecimal max, int passing) {
    }

    /** {@code weightPercent}: el que puso el docente (nulo si no tiene); {@code effectiveWeight}: el que aplica. */
    public record Column(UUID activityId, String title, BigDecimal weightPercent, BigDecimal effectiveWeight,
            Instant closesAt, ColumnStats stats) {
    }

    public record CourseStats(int students, int withPartial, BigDecimal averagePartial, int passingPartial,
            int withDefinitive, BigDecimal averageDefinitive, int passingDefinitive) {
    }

    /** {@code fullView} es falso para un estudiante: solo su fila, sin estadísticas. */
    public record View(UUID courseId, String courseName, String period, List<Column> columns,
            BigDecimal explicitWeightSum, boolean weightsComplete, String weightsWarning, BigDecimal passingScore,
            List<Row> rows, CourseStats stats, boolean fullView) {
    }

    private final ActivityCatalog catalog;
    private final CourseAccess courseAccess;
    private final CourseRoster roster;
    private final GradeRepository grades;

    public Gradebook(ActivityCatalog catalog, CourseAccess courseAccess, CourseRoster roster, GradeRepository grades) {
        this.catalog = catalog;
        this.courseAccess = courseAccess;
        this.roster = roster;
        this.grades = grades;
    }

    @Transactional(readOnly = true)
    public View view(Actor actor, UUID courseId) {
        courseAccess.requireViewable(actor, courseId);
        if (courseAccess.canViewCourseWork(actor, courseId)) {
            return build(courseId, roster.members(courseId), true);
        }
        if (!courseAccess.isEnrolled(actor, courseId)) {
            throw ApplicationException.forbidden("Solo el docente del curso, un Administrador o sus estudiantes ven "
                    + "la planilla.");
        }
        List<CourseRoster.Member> self = roster.members(courseId).stream()
                .filter(m -> m.cedula().equals(actor.cedula())).toList();
        return build(courseId, self, false);
    }

    /** Planilla completa para exportar: docente del curso y cuentas Administrador. */
    @Transactional(readOnly = true)
    public View fullView(Actor actor, UUID courseId) {
        courseAccess.requireViewable(actor, courseId);
        if (!courseAccess.canViewCourseWork(actor, courseId)) {
            throw ApplicationException.forbidden("Solo el docente del curso o un Administrador exporta la planilla.");
        }
        return build(courseId, roster.members(courseId), true);
    }

    private View build(UUID courseId, List<CourseRoster.Member> members, boolean full) {
        CourseAccess.CourseSummary course = courseAccess.summary(courseId);
        List<ActivityCatalog.GradableActivity> activities = catalog.gradableWork(courseId, members);
        GradebookRules.Weights weights = GradebookRules.weights(
                activities.stream().map(ActivityCatalog.GradableActivity::weightPercent).toList());

        List<UUID> lastSubmissions = activities.stream().flatMap(a -> a.work().stream())
                .map(BrowseSubmissions.StudentWork::lastSubmissionId).filter(Objects::nonNull).toList();
        Map<UUID, Grade> bySubmission = grades.findBySubmissions(lastSubmissions).stream()
                .collect(Collectors.toMap(Grade::getSubmissionId, Function.identity()));

        // cells.get(activity).get(member): cada lista de trabajo viene en el orden de members.
        List<List<Cell>> cells = activities.stream().map(a -> a.work().stream()
                .map(w -> cell(a.id(), w, bySubmission)).toList()).toList();

        List<Row> rows = IntStream.range(0, members.size()).mapToObj(m -> {
            CourseRoster.Member member = members.get(m);
            List<Cell> rowCells = cells.stream().map(column -> column.get(m)).toList();
            GradebookRules.Totals totals = GradebookRules.totals(weights,
                    rowCells.stream().map(Cell::result).toList());
            return new Row(member.cedula(), member.fullName(), member.userId() != null, rowCells, totals.partial(),
                    totals.definitive());
        }).toList();

        List<Column> columns = IntStream.range(0, activities.size()).mapToObj(i -> {
            ActivityCatalog.GradableActivity a = activities.get(i);
            return new Column(a.id(), a.title(), a.weightPercent(),
                    weights.effective().get(i).setScale(2, RoundingMode.HALF_UP), a.closesAt(),
                    full ? columnStats(cells.get(i)) : null);
        }).toList();

        return new View(courseId, course.name(), course.period(), columns, weights.explicitSum(), weights.complete(),
                warning(activities.isEmpty(), weights), GradebookRules.PASSING_SCORE, rows,
                full ? courseStats(rows) : null, full);
    }

    private static Cell cell(UUID activityId, BrowseSubmissions.StudentWork work, Map<UUID, Grade> bySubmission) {
        ActivityView.State state = work.progress().state();
        if (state == ActivityView.State.SUBMITTED) {
            Grade grade = bySubmission.get(work.lastSubmissionId());
            return grade != null && grade.isPublished()
                    ? new Cell(activityId, CellStatus.GRADED, grade.getScore(), work.lastSubmissionId())
                    : new Cell(activityId, CellStatus.IN_REVIEW, null, work.lastSubmissionId());
        }
        if (state == ActivityView.State.OVERDUE) {
            return new Cell(activityId, CellStatus.OVERDUE, BigDecimal.ZERO.setScale(1), null);
        }
        return new Cell(activityId, CellStatus.PENDING, null, null);
    }

    private static ColumnStats columnStats(List<Cell> column) {
        List<BigDecimal> results = column.stream().map(Cell::result).filter(Objects::nonNull).toList();
        return new ColumnStats(
                (int) column.stream().filter(c -> c.submissionId() != null).count(),
                count(column, CellStatus.GRADED), count(column, CellStatus.IN_REVIEW),
                count(column, CellStatus.OVERDUE), GradebookRules.average(results),
                results.stream().min(BigDecimal::compareTo).orElse(null),
                results.stream().max(BigDecimal::compareTo).orElse(null),
                (int) results.stream().filter(GradebookRules::passes).count());
    }

    private static CourseStats courseStats(List<Row> rows) {
        List<BigDecimal> partials = rows.stream().map(Row::partial).filter(Objects::nonNull).toList();
        List<BigDecimal> definitives = rows.stream().map(Row::definitive).filter(Objects::nonNull).toList();
        return new CourseStats(rows.size(), partials.size(), GradebookRules.average(partials),
                (int) partials.stream().filter(GradebookRules::passes).count(), definitives.size(),
                GradebookRules.average(definitives),
                (int) definitives.stream().filter(GradebookRules::passes).count());
    }

    private static int count(List<Cell> column, CellStatus status) {
        return (int) column.stream().filter(c -> c.status() == status).count();
    }

    private static String warning(boolean noActivities, GradebookRules.Weights weights) {
        if (noActivities) {
            return "El curso aún no tiene actividades calificables publicadas.";
        }
        if (!weights.complete()) {
            return "Todas las actividades tienen porcentaje y suman " + weights.explicitSum().stripTrailingZeros()
                    .toPlainString().replace('.', ',') + " %. Ajústalos para que sumen 100 % y así calcular la nota "
                    + "definitiva (RN-07).";
        }
        long unweighted = weights.effective().stream().filter(w -> w.signum() == 0).count();
        if (unweighted > 0) {
            return unweighted + (unweighted == 1 ? " actividad sin porcentaje no cuenta" : " actividades sin porcentaje no "
                    + "cuentan") + " en la nota porque los porcentajes ya suman 100 %.";
        }
        return null;
    }
}
