package co.edu.unipamplona.logicode.activities.api;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.ActivityView;
import co.edu.unipamplona.logicode.activities.application.BrowseSubmissions;
import co.edu.unipamplona.logicode.activities.application.ManageActivities;
import co.edu.unipamplona.logicode.activities.application.ManageDeadlineExceptions;
import co.edu.unipamplona.logicode.activities.application.SubmissionView;
import co.edu.unipamplona.logicode.activities.application.SubmitWork;
import co.edu.unipamplona.logicode.activities.domain.Activity;
import co.edu.unipamplona.logicode.activities.domain.ActivityKind;
import co.edu.unipamplona.logicode.activities.domain.FileGroup;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.CurrentActor;
import co.edu.unipamplona.logicode.shared.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ApiPaths.V1)
@Tag(name = "Actividades", description = "Actividades, entregas y excepciones de plazo (RF-10, RF-11, RF-13, RF-19)")
public class ActivityController {

    public record ActivityRequest(@NotBlank @Size(max = 150) String title, @NotNull ActivityKind kind,
            @NotBlank @Size(max = 20000) String statement, boolean acceptsText, boolean acceptsFiles,
            boolean acceptsPhotos, Set<FileGroup> fileGroups, @Digits(integer = 3, fraction = 2) BigDecimal weightPercent,
            @NotNull Instant opensAt, @NotNull Instant closesAt, @Min(1) @Max(Activity.MAX_ATTEMPTS) int maxAttempts) {

        Activity.Definition definition() {
            return new Activity.Definition(title, kind, statement, acceptsText, acceptsFiles, acceptsPhotos,
                    fileGroups == null ? Set.of() : fileGroups, weightPercent, opensAt, closesAt, maxAttempts);
        }
    }

    /** Sin {@code extraAttempts} no se dan intentos adicionales. */
    public record ExceptionRequest(Instant extendedUntil, @Min(0) @Max(Activity.MAX_ATTEMPTS) Integer extraAttempts,
            @Size(max = 500) String reason) {

        int attempts() {
            return extraAttempts == null ? 0 : extraAttempts;
        }
    }

    private final ManageActivities manageActivities;
    private final SubmitWork submitWork;
    private final BrowseSubmissions browseSubmissions;
    private final ManageDeadlineExceptions manageExceptions;

    public ActivityController(ManageActivities manageActivities, SubmitWork submitWork,
            BrowseSubmissions browseSubmissions, ManageDeadlineExceptions manageExceptions) {
        this.manageActivities = manageActivities;
        this.submitWork = submitWork;
        this.browseSubmissions = browseSubmissions;
        this.manageExceptions = manageExceptions;
    }

    @GetMapping("/courses/{courseId}/activities")
    @Operation(summary = "Actividades del curso por fecha de cierre (borradores solo para el docente)")
    public List<ActivityView> list(@CurrentActor Actor actor, @PathVariable UUID courseId) {
        return manageActivities.list(actor, courseId);
    }

    @PostMapping("/courses/{courseId}/activities")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una actividad como borrador")
    public ActivityView create(@CurrentActor Actor actor, @PathVariable UUID courseId,
            @Valid @RequestBody ActivityRequest request) {
        return manageActivities.create(actor, courseId, request.definition());
    }

    @GetMapping("/activities/{activityId}")
    @Operation(summary = "Detalle de una actividad; para un estudiante inscrito incluye su avance")
    public ActivityView get(@CurrentActor Actor actor, @PathVariable UUID activityId) {
        return manageActivities.get(actor, activityId);
    }

    @PutMapping("/activities/{activityId}")
    @Operation(summary = "Editar una actividad; si cambia el enunciado se crea una versión nueva de la rúbrica")
    public ActivityView update(@CurrentActor Actor actor, @PathVariable UUID activityId,
            @Valid @RequestBody ActivityRequest request) {
        return manageActivities.update(actor, activityId, request.definition());
    }

    @PostMapping("/activities/{activityId}/publish")
    @Operation(summary = "Publicar la actividad (requiere rúbrica, RN-20)")
    public ActivityView publish(@CurrentActor Actor actor, @PathVariable UUID activityId) {
        return manageActivities.publish(actor, activityId);
    }

    @DeleteMapping("/activities/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archivar la actividad; las entregas se conservan")
    public void archive(@CurrentActor Actor actor, @PathVariable UUID activityId) {
        manageActivities.archive(actor, activityId);
    }

    @PostMapping(path = "/activities/{activityId}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Entregar: texto (text) y hasta 5 archivos o fotos (files)")
    public SubmissionView submit(@CurrentActor Actor actor, @PathVariable UUID activityId,
            @RequestParam(required = false) @Size(max = 50000) String text,
            @RequestPart(name = "files", required = false) List<MultipartFile> files) throws IOException {
        List<MultipartFile> received = files == null ? List.of()
                : files.stream().filter(f -> !f.isEmpty()).toList();
        List<InputStream> opened = new ArrayList<>();
        try {
            List<SubmitWork.Upload> uploads = new ArrayList<>();
            for (MultipartFile file : received) {
                InputStream content = file.getInputStream();
                opened.add(content);
                uploads.add(new SubmitWork.Upload(content, file.getOriginalFilename()));
            }
            return submitWork.submit(actor, activityId, text, uploads);
        } finally {
            for (InputStream content : opened) {
                content.close();
            }
        }
    }

    @GetMapping("/activities/{activityId}/submissions/mine")
    @Operation(summary = "Mis intentos en la actividad, del más reciente al más antiguo")
    public List<SubmissionView> mine(@CurrentActor Actor actor, @PathVariable UUID activityId) {
        return browseSubmissions.mine(actor, activityId);
    }

    @GetMapping("/activities/{activityId}/submissions")
    @Operation(summary = "Inscritos con su situación en la actividad (docente del curso o Administrador)")
    public List<BrowseSubmissions.StudentWork> roster(@CurrentActor Actor actor, @PathVariable UUID activityId) {
        return browseSubmissions.forActivity(actor, activityId);
    }

    @PutMapping("/activities/{activityId}/exceptions/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Ampliar el plazo o dar intentos adicionales a un estudiante (RN-05)")
    public void grantException(@CurrentActor Actor actor, @PathVariable UUID activityId,
            @PathVariable UUID studentId, @Valid @RequestBody ExceptionRequest request) {
        manageExceptions.grant(actor, activityId, studentId, request.extendedUntil(), request.attempts(),
                request.reason());
    }

    @DeleteMapping("/activities/{activityId}/exceptions/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quitar la excepción de plazo de un estudiante")
    public void revokeException(@CurrentActor Actor actor, @PathVariable UUID activityId,
            @PathVariable UUID studentId) {
        manageExceptions.revoke(actor, activityId, studentId);
    }
}
