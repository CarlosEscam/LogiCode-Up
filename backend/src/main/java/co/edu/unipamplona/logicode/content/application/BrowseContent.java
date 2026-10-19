package co.edu.unipamplona.logicode.content.application;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.edu.unipamplona.logicode.content.domain.Material;
import co.edu.unipamplona.logicode.content.domain.MaterialKind;
import co.edu.unipamplona.logicode.content.domain.Tool;
import co.edu.unipamplona.logicode.content.domain.Topic;
import co.edu.unipamplona.logicode.courses.application.CourseAccess;
import co.edu.unipamplona.logicode.files.application.FileStore;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas de contenido: temas de un curso, biblioteca con búsqueda y filtros (RF-24), detalle, previsualización y
 * descarga (RF-25). El actor es nulo para un visitante, que solo ve lo público (RN-03, RN-17).
 * <p>
 * Lo que el actor no puede ver responde como inexistente (404), para no revelar contenido privado.
 */
@Service
public class BrowseContent {

    public record LibraryFilter(String text, Tool tool, UUID topicId, MaterialKind kind) {
    }

    /** Tema que aparece en los filtros de la biblioteca. */
    public record LibraryTopic(UUID id, String title, Tool tool, String courseName) {
    }

    private final TopicRepository topics;
    private final MaterialRepository materials;
    private final CourseAccess courseAccess;
    private final FileStore fileStore;
    private final ContentViews views;

    public BrowseContent(TopicRepository topics, MaterialRepository materials, CourseAccess courseAccess,
            FileStore fileStore, ContentViews views) {
        this.topics = topics;
        this.materials = materials;
        this.courseAccess = courseAccess;
        this.fileStore = fileStore;
        this.views = views;
    }

    /** Temas del curso con todos sus materiales; solo para quien participa en el curso. */
    @Transactional(readOnly = true)
    public List<TopicView> courseTopics(Actor actor, UUID courseId) {
        courseAccess.requireViewable(actor, courseId);
        List<Topic> courseTopics = topics.findActiveByCourse(courseId);
        List<Material> courseMaterials = materials.findActiveByTopics(courseTopics.stream().map(Topic::getId)
                .toList());
        return views.topics(actor, courseTopics, courseMaterials);
    }

    @Transactional(readOnly = true)
    public PageResult<MaterialView> library(Actor actor, LibraryFilter filter, int page, int size) {
        List<Topic> candidates = topics.findAllActive().stream()
                .filter(t -> filter.tool() == null || t.getTool() == filter.tool())
                .filter(t -> filter.topicId() == null || t.getId().equals(filter.topicId()))
                .toList();
        String text = Texts.optional(filter.text());
        Set<UUID> textTopics = text == null ? Set.of() : candidates.stream()
                .filter(t -> t.getTitle().toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT)))
                .map(Topic::getId).collect(Collectors.toSet());
        MaterialRepository.Search search = scope(actor, candidates, filter.kind(), text, textTopics);
        if (search.topicIds().isEmpty()) {
            return new PageResult<>(List.of(), Math.max(page, 0), Math.clamp(size, 1, 50), 0, 0);
        }
        PageResult<Material> result = materials.search(search, Math.max(page, 0), Math.clamp(size, 1, 50));
        Map<UUID, Topic> byId = candidates.stream().collect(Collectors.toMap(Topic::getId, Function.identity()));
        return new PageResult<>(views.materials(actor, result.items(), byId), result.page(), result.size(),
                result.totalItems(), result.totalPages());
    }

    /** Temas con por lo menos un material visible para el actor, para los filtros de la biblioteca. */
    @Transactional(readOnly = true)
    public List<LibraryTopic> libraryTopics(Actor actor) {
        List<Topic> all = topics.findAllActive();
        if (all.isEmpty()) {
            return List.of();
        }
        Set<UUID> withMaterials = materials.topicsWithMaterials(scope(actor, all, null, null, Set.of()));
        return all.stream().filter(t -> withMaterials.contains(t.getId()))
                .sorted(Comparator.comparing(Topic::getTool).thenComparing(Topic::getPosition))
                .map(t -> new LibraryTopic(t.getId(), t.getTitle(), t.getTool(),
                        courseAccess.summary(t.getCourseId()).name()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MaterialView material(Actor actor, UUID materialId) {
        Material material = requireMaterial(materialId);
        return views.material(actor, material, requireVisible(actor, material));
    }

    /**
     * Contenido del archivo de un material. Con {@code preview} solo se entregan formatos que el navegador muestra
     * dentro de la página (PDF, imágenes y videos).
     */
    @Transactional(readOnly = true)
    public FileStore.FileContent openFile(Actor actor, UUID materialId, boolean preview) {
        Material material = requireMaterial(materialId);
        requireVisible(actor, material);
        if (material.getFileId() == null) {
            throw ApplicationException.notFound("Este material es un enlace y no tiene archivo.");
        }
        FileStore.FileContent content = fileStore.open(material.getFileId());
        if (preview && !content.info().previewable()) {
            throw ApplicationException.businessRule("Este formato no tiene vista previa; descárgalo para abrirlo.");
        }
        return content;
    }

    private MaterialRepository.Search scope(Actor actor, List<Topic> candidates, MaterialKind kind, String text,
            Set<UUID> textTopics) {
        Set<UUID> viewableCourses = courseAccess.viewableCourseIds(actor);
        Set<UUID> topicIds = candidates.stream().map(Topic::getId).collect(Collectors.toSet());
        Set<UUID> privateTopics = candidates.stream().filter(t -> viewableCourses.contains(t.getCourseId()))
                .map(Topic::getId).collect(Collectors.toSet());
        return new MaterialRepository.Search(topicIds, privateTopics, kind, text, textTopics);
    }

    private Material requireMaterial(UUID materialId) {
        return materials.findById(materialId).filter(m -> !m.isArchived())
                .orElseThrow(() -> ApplicationException.notFound("El material no existe."));
    }

    private Topic requireVisible(Actor actor, Material material) {
        Topic topic = topics.findById(material.getTopicId()).filter(t -> !t.isArchived())
                .orElseThrow(() -> ApplicationException.notFound("El material no existe."));
        if (!material.isPublic() && !courseAccess.canViewPrivate(actor, topic.getCourseId())) {
            throw ApplicationException.notFound("El material no existe.");
        }
        return topic;
    }
}
