package co.edu.unipamplona.logicode.grading.api;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;

import co.edu.unipamplona.logicode.grading.application.Gradebook;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.CurrentActor;
import co.edu.unipamplona.logicode.shared.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/courses/{courseId}/gradebook")
@Tag(name = "Planilla", description = "Planilla del curso con pesos, avance parcial, nota definitiva, estadísticas y exportación (RF-18, RF-20, RN-07)")
public class GradebookController {

    private final Gradebook gradebook;
    private final Clock clock;

    public GradebookController(Gradebook gradebook, Clock clock) {
        this.gradebook = gradebook;
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "Planilla: completa para el docente y las cuentas Administrador; la fila propia para un estudiante")
    public Gradebook.View view(@CurrentActor Actor actor, @PathVariable UUID courseId) {
        return gradebook.view(actor, courseId);
    }

    @GetMapping("/export")
    @Operation(summary = "Planilla en CSV para Excel (punto y coma, coma decimal, UTF-8)")
    public ResponseEntity<byte[]> export(@CurrentActor Actor actor, @PathVariable UUID courseId) {
        Gradebook.View view = gradebook.fullView(actor, courseId);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName(view, ".csv"), StandardCharsets.UTF_8).build().toString())
                .body(GradebookCsv.write(view, clock).getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/export/xlsx")
    @Operation(summary = "Planilla en Excel .xlsx, con las notas y los porcentajes como números")
    public ResponseEntity<byte[]> exportXlsx(@CurrentActor Actor actor, @PathVariable UUID courseId) {
        Gradebook.View view = gradebook.fullView(actor, courseId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName(view, ".xlsx"), StandardCharsets.UTF_8).build().toString())
                .body(GradebookXlsx.write(view, clock));
    }

    private static String fileName(Gradebook.View view, String extension) {
        return ("planilla-" + view.courseName() + "-" + view.period()).replaceAll("[^\\p{L}\\p{N}-]+", "-")
                + extension;
    }
}
