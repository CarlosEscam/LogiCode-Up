package co.edu.unipamplona.logicode.activities.api;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.BrowseSubmissions;
import co.edu.unipamplona.logicode.activities.application.SubmissionView;
import co.edu.unipamplona.logicode.files.application.FileStore;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.CurrentActor;
import co.edu.unipamplona.logicode.shared.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/submissions")
@Tag(name = "Entregas", description = "Detalle y archivos de una entrega (RF-13, RF-17)")
public class SubmissionController {

    private final BrowseSubmissions browseSubmissions;

    public SubmissionController(BrowseSubmissions browseSubmissions) {
        this.browseSubmissions = browseSubmissions;
    }

    @GetMapping("/{submissionId}")
    @Operation(summary = "Detalle de una entrega (su autor, el docente del curso o un Administrador)")
    public SubmissionView get(@CurrentActor Actor actor, @PathVariable UUID submissionId) {
        return browseSubmissions.get(actor, submissionId);
    }

    @GetMapping("/{submissionId}/files/{fileId}/preview")
    @Operation(summary = "Ver un archivo de la entrega dentro de la página (PDF, imágenes y videos)")
    public ResponseEntity<Resource> preview(@CurrentActor Actor actor, @PathVariable UUID submissionId,
            @PathVariable UUID fileId) {
        return fileResponse(browseSubmissions.openFile(actor, submissionId, fileId, true),
                ContentDisposition.inline());
    }

    @GetMapping("/{submissionId}/files/{fileId}/download")
    @Operation(summary = "Descargar un archivo de la entrega")
    public ResponseEntity<Resource> download(@CurrentActor Actor actor, @PathVariable UUID submissionId,
            @PathVariable UUID fileId) {
        return fileResponse(browseSubmissions.openFile(actor, submissionId, fileId, false),
                ContentDisposition.attachment());
    }

    private static ResponseEntity<Resource> fileResponse(FileStore.FileContent content,
            ContentDisposition.Builder disposition) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.info().contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition.filename(content.info().originalName(), StandardCharsets.UTF_8).build().toString())
                .cacheControl(CacheControl.noCache().cachePrivate())
                .body(content.resource());
    }
}
