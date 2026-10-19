package co.edu.unipamplona.logicode.content.api;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.content.application.BrowseContent;
import co.edu.unipamplona.logicode.content.application.ManageTopics;
import co.edu.unipamplona.logicode.content.application.TopicView;
import co.edu.unipamplona.logicode.content.domain.Tool;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.CurrentActor;
import co.edu.unipamplona.logicode.shared.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1)
@Tag(name = "Temas", description = "Temas de un curso y próxima clase (RF-06, RF-09)")
public class TopicController {

    public record TopicRequest(@NotBlank @Size(max = 150) String title, @Size(max = 2000) String description,
            @NotNull Tool tool) {
    }

    public record OrderRequest(@NotNull List<UUID> ids) {
    }

    public record NextClassRequest(UUID topicId) {
    }

    private final BrowseContent browseContent;
    private final ManageTopics manageTopics;

    public TopicController(BrowseContent browseContent, ManageTopics manageTopics) {
        this.browseContent = browseContent;
        this.manageTopics = manageTopics;
    }

    @GetMapping("/courses/{courseId}/topics")
    @Operation(summary = "Temas del curso con sus materiales, en orden")
    public List<TopicView> list(@CurrentActor Actor actor, @PathVariable UUID courseId) {
        return browseContent.courseTopics(actor, courseId);
    }

    @PostMapping("/courses/{courseId}/topics")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un tema al final del curso")
    public TopicView create(@CurrentActor Actor actor, @PathVariable UUID courseId,
            @Valid @RequestBody TopicRequest request) {
        return manageTopics.create(actor, courseId, request.title(), request.description(), request.tool());
    }

    @PutMapping("/courses/{courseId}/topics/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reordenar los temas del curso (lista completa de ids en el nuevo orden)")
    public void reorder(@CurrentActor Actor actor, @PathVariable UUID courseId,
            @Valid @RequestBody OrderRequest request) {
        manageTopics.reorder(actor, courseId, request.ids());
    }

    @PutMapping("/courses/{courseId}/next-class")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marcar el tema de la próxima clase (topicId nulo quita la marca)")
    public void setNextClass(@CurrentActor Actor actor, @PathVariable UUID courseId,
            @RequestBody NextClassRequest request) {
        manageTopics.setNextClass(actor, courseId, request.topicId());
    }

    @PutMapping("/topics/{topicId}")
    @Operation(summary = "Editar un tema")
    public TopicView update(@CurrentActor Actor actor, @PathVariable UUID topicId,
            @Valid @RequestBody TopicRequest request) {
        return manageTopics.update(actor, topicId, request.title(), request.description(), request.tool());
    }

    @DeleteMapping("/topics/{topicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archivar un tema (deja de mostrarse con sus materiales)")
    public void archive(@CurrentActor Actor actor, @PathVariable UUID topicId) {
        manageTopics.archive(actor, topicId);
    }
}
