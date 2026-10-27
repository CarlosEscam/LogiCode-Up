package co.edu.unipamplona.logicode.forum.api;

import java.util.UUID;

import co.edu.unipamplona.logicode.forum.application.BrowseForum;
import co.edu.unipamplona.logicode.forum.application.ForumViews;
import co.edu.unipamplona.logicode.forum.application.ModerateForum;
import co.edu.unipamplona.logicode.forum.application.ParticipateInForum;
import co.edu.unipamplona.logicode.forum.domain.ForumCategory;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.CurrentActor;
import co.edu.unipamplona.logicode.shared.api.ApiPaths;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/forum")
@Tag(name = "Foro", description = "Hilos por herramienta: lectura pública, escritura con sesión y moderación (RF-26, RF-27)")
public class ForumController {

    public record ThreadRequest(@NotNull ForumCategory category, @NotBlank @Size(min = 3, max = 150) String title,
            @NotBlank @Size(max = 10000) String body) {
    }

    public record ReplyRequest(@NotBlank @Size(max = 10000) String body) {
    }

    public record ModerationRequest(Boolean pinned, Boolean closed) {
    }

    private final BrowseForum browse;
    private final ParticipateInForum participate;
    private final ModerateForum moderate;

    public ForumController(BrowseForum browse, ParticipateInForum participate, ModerateForum moderate) {
        this.browse = browse;
        this.participate = participate;
        this.moderate = moderate;
    }

    @GetMapping("/threads")
    @Operation(summary = "Hilos (fijados primero, luego por última actividad); filtra por herramienta y texto")
    public PageResult<ForumViews.ThreadSummary> threads(@RequestParam(required = false) ForumCategory category,
            @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return browse.threads(category, q, page, size);
    }

    @GetMapping("/threads/{threadId}")
    @Operation(summary = "Un hilo con sus respuestas")
    public ForumViews.ThreadDetail thread(@CurrentActor(required = false) Actor actor, @PathVariable UUID threadId) {
        return browse.thread(actor, threadId);
    }

    @PostMapping("/threads")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abrir un hilo (con sesión)")
    public ForumViews.ThreadDetail start(@CurrentActor Actor actor, @Valid @RequestBody ThreadRequest request) {
        return participate.startThread(actor, request.category(), request.title(), request.body());
    }

    @PostMapping("/threads/{threadId}/posts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Responder en un hilo abierto (con sesión); 409 si está cerrado")
    public ForumViews.ThreadDetail reply(@CurrentActor Actor actor, @PathVariable UUID threadId,
            @Valid @RequestBody ReplyRequest request) {
        return participate.reply(actor, threadId, request.body());
    }

    @PatchMapping("/threads/{threadId}")
    @Operation(summary = "Fijar o cerrar un hilo (moderación: docente y Administrador)")
    public ForumViews.ThreadDetail update(@CurrentActor Actor actor, @PathVariable UUID threadId,
            @RequestBody ModerationRequest request) {
        return moderate.update(actor, threadId, request.pinned(), request.closed());
    }

    @DeleteMapping("/threads/{threadId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un hilo (moderación)")
    public void deleteThread(@CurrentActor Actor actor, @PathVariable UUID threadId) {
        moderate.deleteThread(actor, threadId);
    }

    @DeleteMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar una respuesta (moderación)")
    public void deletePost(@CurrentActor Actor actor, @PathVariable UUID postId) {
        moderate.deletePost(actor, postId);
    }
}
