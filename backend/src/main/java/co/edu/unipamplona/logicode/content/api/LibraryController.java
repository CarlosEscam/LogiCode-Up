package co.edu.unipamplona.logicode.content.api;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.content.application.BrowseContent;
import co.edu.unipamplona.logicode.content.application.MaterialView;
import co.edu.unipamplona.logicode.content.domain.MaterialKind;
import co.edu.unipamplona.logicode.content.domain.Tool;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.CurrentActor;
import co.edu.unipamplona.logicode.shared.api.ApiPaths;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/library")
@Tag(name = "Biblioteca", description = "Biblioteca por tema y herramienta (RF-24). Abierta a visitantes.")
public class LibraryController {

    private final BrowseContent browseContent;

    public LibraryController(BrowseContent browseContent) {
        this.browseContent = browseContent;
    }

    @GetMapping
    @Operation(summary = "Buscar materiales: los visitantes ven solo lo público; quien participa en un curso, "
            + "también lo privado de ese curso")
    public PageResult<MaterialView> search(@CurrentActor(required = false) Actor actor,
            @RequestParam(required = false) String q, @RequestParam(required = false) Tool tool,
            @RequestParam(required = false) UUID topicId, @RequestParam(required = false) MaterialKind kind,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return browseContent.library(actor, new BrowseContent.LibraryFilter(q, tool, topicId, kind), page, size);
    }

    @GetMapping("/topics")
    @Operation(summary = "Temas con materiales visibles, para los filtros")
    public List<BrowseContent.LibraryTopic> topics(@CurrentActor(required = false) Actor actor) {
        return browseContent.libraryTopics(actor);
    }
}
