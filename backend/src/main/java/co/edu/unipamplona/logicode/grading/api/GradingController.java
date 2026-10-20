package co.edu.unipamplona.logicode.grading.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.application.BrowseGrades;
import co.edu.unipamplona.logicode.grading.application.GradeDetail;
import co.edu.unipamplona.logicode.grading.application.GradeSubmissions;
import co.edu.unipamplona.logicode.grading.application.ManageRubrics;
import co.edu.unipamplona.logicode.grading.application.RubricView;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.CurrentActor;
import co.edu.unipamplona.logicode.shared.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1)
@Tag(name = "Calificación", description = "Rúbricas versionadas, bandeja Por revisar, notas e historial (RF-17, RN-20)")
public class GradingController {

    public record CriterionRequest(@NotBlank @Size(max = 150) String name, @Size(max = 2000) String description,
            @NotNull @Digits(integer = 4, fraction = 2) BigDecimal maxPoints) {
    }

    public record RubricRequest(@Size(max = 20000) String expectedAnswer,
            @NotEmpty @Size(max = 20) List<@Valid CriterionRequest> criteria) {
    }

    public record GradeRequest(@NotNull @Digits(integer = 1, fraction = 1) BigDecimal score,
            @Size(max = 5000) String feedback) {
    }

    private final ManageRubrics manageRubrics;
    private final GradeSubmissions gradeSubmissions;
    private final BrowseGrades browseGrades;

    public GradingController(ManageRubrics manageRubrics, GradeSubmissions gradeSubmissions,
            BrowseGrades browseGrades) {
        this.manageRubrics = manageRubrics;
        this.gradeSubmissions = gradeSubmissions;
        this.browseGrades = browseGrades;
    }

    @GetMapping("/activities/{activityId}/rubric")
    @Operation(summary = "Rúbrica vigente (204 si aún no tiene); el estudiante no ve la respuesta esperada")
    public ResponseEntity<RubricView> currentRubric(@CurrentActor Actor actor, @PathVariable UUID activityId) {
        return manageRubrics.current(actor, activityId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/activities/{activityId}/rubric")
    @Operation(summary = "Definir la rúbrica: guarda una versión nueva")
    public RubricView defineRubric(@CurrentActor Actor actor, @PathVariable UUID activityId,
            @Valid @RequestBody RubricRequest request) {
        return manageRubrics.define(actor, activityId, request.expectedAnswer(), request.criteria().stream()
                .map(c -> new ManageRubrics.CriterionInput(c.name(), c.description(), c.maxPoints())).toList());
    }

    @GetMapping("/activities/{activityId}/rubric/versions")
    @Operation(summary = "Versiones de la rúbrica, de la más reciente a la más antigua")
    public List<RubricView> rubricVersions(@CurrentActor Actor actor, @PathVariable UUID activityId) {
        return manageRubrics.versions(actor, activityId);
    }

    @GetMapping("/courses/{courseId}/review-queue")
    @Operation(summary = "Bandeja Por revisar del curso")
    public List<BrowseGrades.ReviewItem> reviewQueue(@CurrentActor Actor actor, @PathVariable UUID courseId) {
        return browseGrades.reviewQueue(actor, courseId);
    }

    @GetMapping("/courses/{courseId}/grades/mine")
    @Operation(summary = "Mis notas en el curso (último intento de cada actividad)")
    public List<BrowseGrades.MyGrade> myGrades(@CurrentActor Actor actor, @PathVariable UUID courseId) {
        return browseGrades.mine(actor, courseId);
    }

    @GetMapping("/activities/{activityId}/grades")
    @Operation(summary = "Intentos de la actividad con su nota (docente del curso o Administrador)")
    public List<BrowseGrades.GradeRow> activityGrades(@CurrentActor Actor actor, @PathVariable UUID activityId) {
        return browseGrades.forActivity(actor, activityId);
    }

    @GetMapping("/submissions/{submissionId}/grade")
    @Operation(summary = "Nota de la entrega con su rúbrica; el historial solo para el docente y Administradores")
    public GradeDetail grade(@CurrentActor Actor actor, @PathVariable UUID submissionId) {
        return gradeSubmissions.get(actor, submissionId);
    }

    @PostMapping("/submissions/{submissionId}/grade")
    @Operation(summary = "Calificar o corregir la nota (0,0 a 5,0) con retroalimentación")
    public GradeDetail publishGrade(@CurrentActor Actor actor, @PathVariable UUID submissionId,
            @Valid @RequestBody GradeRequest request) {
        return gradeSubmissions.grade(actor, submissionId, request.score(), request.feedback());
    }
}
