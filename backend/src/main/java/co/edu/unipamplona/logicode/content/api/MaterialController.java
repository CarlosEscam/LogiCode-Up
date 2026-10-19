package co.edu.unipamplona.logicode.content.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import co.edu.unipamplona.logicode.content.application.BrowseContent;
import co.edu.unipamplona.logicode.content.application.ManageMaterials;
import co.edu.unipamplona.logicode.content.application.MaterialView;
import co.edu.unipamplona.logicode.content.domain.MaterialKind;
import co.edu.unipamplona.logicode.content.domain.Visibility;
import co.edu.unipamplona.logicode.files.application.FileStore;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.CurrentActor;
import co.edu.unipamplona.logicode.shared.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@Tag(name = "Materiales", description = "Materiales de un tema, previsualización y descarga (RF-07, RF-08, RF-25)")
public class MaterialController {

    public record MaterialUpdate(@NotBlank @Size(max = 150) String title, @Size(max = 2000) String description,
            @NotNull Visibility visibility) {
    }

    public record VisibilityRequest(@NotNull Visibility visibility) {
    }

    private final BrowseContent browseContent;
    private final ManageMaterials manageMaterials;

    public MaterialController(BrowseContent browseContent, ManageMaterials manageMaterials) {
        this.browseContent = browseContent;
        this.manageMaterials = manageMaterials;
    }

    @PostMapping(path = "/topics/{topicId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar un material: archivo (file) o enlace (url, con kind=LINK)")
    public MaterialView create(@CurrentActor Actor actor, @PathVariable UUID topicId,
            @RequestParam @NotBlank @Size(max = 150) String title,
            @RequestParam(required = false) @Size(max = 2000) String description,
            @RequestParam MaterialKind kind, @RequestParam Visibility visibility,
            @RequestParam(required = false) String url,
            @RequestPart(name = "file", required = false) MultipartFile file) throws IOException {
        ManageMaterials.NewMaterial data = new ManageMaterials.NewMaterial(title, description, kind, visibility, url);
        if (file == null || file.isEmpty()) {
            return manageMaterials.create(actor, topicId, data, null);
        }
        try (var content = file.getInputStream()) {
            return manageMaterials.create(actor, topicId, data,
                    new ManageMaterials.Upload(content, file.getOriginalFilename()));
        }
    }

    @PutMapping("/topics/{topicId}/materials/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reordenar los materiales del tema (lista completa de ids en el nuevo orden)")
    public void reorder(@CurrentActor Actor actor, @PathVariable UUID topicId,
            @Valid @RequestBody TopicController.OrderRequest request) {
        manageMaterials.reorder(actor, topicId, request.ids());
    }

    @GetMapping("/materials/{materialId}")
    @Operation(summary = "Detalle de un material (público para visitantes; privado para el curso)")
    public MaterialView get(@CurrentActor(required = false) Actor actor, @PathVariable UUID materialId) {
        return browseContent.material(actor, materialId);
    }

    @GetMapping("/materials/{materialId}/preview")
    @Operation(summary = "Ver el archivo dentro de la página (PDF, imágenes y videos; admite Range)")
    public ResponseEntity<Resource> preview(@CurrentActor(required = false) Actor actor,
            @PathVariable UUID materialId) {
        return fileResponse(browseContent.openFile(actor, materialId, true), ContentDisposition.inline());
    }

    @GetMapping("/materials/{materialId}/download")
    @Operation(summary = "Descargar el archivo del material")
    public ResponseEntity<Resource> download(@CurrentActor(required = false) Actor actor,
            @PathVariable UUID materialId) {
        return fileResponse(browseContent.openFile(actor, materialId, false), ContentDisposition.attachment());
    }

    @PutMapping("/materials/{materialId}")
    @Operation(summary = "Editar título, descripción y visibilidad")
    public MaterialView update(@CurrentActor Actor actor, @PathVariable UUID materialId,
            @Valid @RequestBody MaterialUpdate request) {
        return manageMaterials.update(actor, materialId, request.title(), request.description(),
                request.visibility());
    }

    @PutMapping("/materials/{materialId}/visibility")
    @Operation(summary = "Hacer público o privado (docente del curso o Administrador)")
    public MaterialView changeVisibility(@CurrentActor Actor actor, @PathVariable UUID materialId,
            @Valid @RequestBody VisibilityRequest request) {
        return manageMaterials.changeVisibility(actor, materialId, request.visibility());
    }

    @DeleteMapping("/materials/{materialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archivar un material (docente del curso o Administrador)")
    public void archive(@CurrentActor Actor actor, @PathVariable UUID materialId) {
        manageMaterials.archive(actor, materialId);
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
