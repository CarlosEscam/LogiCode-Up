package co.edu.unipamplona.logicode.content.application;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.edu.unipamplona.logicode.content.domain.Material;
import co.edu.unipamplona.logicode.content.domain.MaterialKind;
import co.edu.unipamplona.logicode.content.domain.Topic;
import co.edu.unipamplona.logicode.content.domain.Visibility;
import co.edu.unipamplona.logicode.courses.application.CourseAccess;
import co.edu.unipamplona.logicode.files.application.FileStore;
import co.edu.unipamplona.logicode.files.application.StoredFileInfo;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-07 y RF-08: el docente del curso carga materiales (archivo o enlace), los edita, los ordena y elige si son
 * públicos o privados. Una cuenta Administrador puede cambiar la visibilidad y archivar materiales para mantener la
 * biblioteca, pero no crearlos ni editarlos (RN-04).
 */
@Service
public class ManageMaterials {

    /** Datos de un material nuevo. {@code url} solo para {@link MaterialKind#LINK}. */
    public record NewMaterial(String title, String description, MaterialKind kind, Visibility visibility,
            String url) {
    }

    /** Archivo recibido; nulo para un enlace. */
    public record Upload(InputStream content, String originalName) {
    }

    private static final int MAX_URL_LENGTH = 2000;

    private final MaterialRepository materials;
    private final TopicRepository topics;
    private final CourseAccess courseAccess;
    private final FileStore fileStore;
    private final ContentViews views;
    private final Clock clock;

    public ManageMaterials(MaterialRepository materials, TopicRepository topics, CourseAccess courseAccess,
            FileStore fileStore, ContentViews views, Clock clock) {
        this.materials = materials;
        this.topics = topics;
        this.courseAccess = courseAccess;
        this.fileStore = fileStore;
        this.views = views;
        this.clock = clock;
    }

    @Transactional
    public MaterialView create(Actor actor, UUID topicId, NewMaterial data, Upload upload) {
        Topic topic = requireActiveTopic(topicId);
        courseAccess.requireManageContent(actor, topic.getCourseId());
        if (data.kind() == null) {
            throw ApplicationException.businessRule("Elige el tipo de material.");
        }
        String title = Texts.required(data.title(), "El título del material es obligatorio.");
        String description = Texts.optional(data.description());
        Visibility visibility = requireVisibility(data.visibility());
        int position = materials.nextPosition(topicId);
        Instant now = clock.instant();
        Material material;
        if (data.kind().isLink()) {
            if (upload != null) {
                throw ApplicationException.businessRule("Un enlace no lleva archivo.");
            }
            material = Material.link(topicId, title, description, visibility, position, validUrl(data.url()),
                    actor.userId(), now);
        } else {
            if (upload == null) {
                throw ApplicationException.businessRule("Adjunta el archivo del material.");
            }
            StoredFileInfo file = fileStore.store(upload.content(), upload.originalName(), actor.userId(),
                    Set.of(MaterialFormats.acceptedFor(data.kind())));
            material = Material.file(topicId, title, description, data.kind(), visibility, position, file.id(),
                    actor.userId(), now);
        }
        return views.material(actor, materials.save(material), topic);
    }

    @Transactional
    public MaterialView update(Actor actor, UUID materialId, String title, String description,
            Visibility visibility) {
        Material material = requireActive(materialId);
        Topic topic = requireActiveTopic(material.getTopicId());
        courseAccess.requireManageContent(actor, topic.getCourseId());
        material.update(Texts.required(title, "El título del material es obligatorio."), Texts.optional(description),
                requireVisibility(visibility), clock.instant());
        return views.material(actor, materials.save(material), topic);
    }

    @Transactional
    public MaterialView changeVisibility(Actor actor, UUID materialId, Visibility visibility) {
        Material material = requireActive(materialId);
        Topic topic = requireActiveTopic(material.getTopicId());
        requireModerate(actor, topic);
        material.changeVisibility(requireVisibility(visibility), clock.instant());
        return views.material(actor, materials.save(material), topic);
    }

    /** Archiva el material: deja de mostrarse; el archivo se conserva al menos el tiempo de retención (RN-21). */
    @Transactional
    public void archive(Actor actor, UUID materialId) {
        Material material = requireActive(materialId);
        Topic topic = requireActiveTopic(material.getTopicId());
        requireModerate(actor, topic);
        material.archive(clock.instant());
        materials.save(material);
    }

    /** Recibe todos los materiales del tema en el nuevo orden. */
    @Transactional
    public void reorder(Actor actor, UUID topicId, List<UUID> orderedIds) {
        Topic topic = requireActiveTopic(topicId);
        courseAccess.requireManageContent(actor, topic.getCourseId());
        Map<UUID, Material> current = materials.findActiveByTopics(List.of(topicId)).stream()
                .collect(Collectors.toMap(Material::getId, Function.identity()));
        if (orderedIds == null || orderedIds.size() != current.size()
                || !new HashSet<>(orderedIds).equals(current.keySet())) {
            throw ApplicationException.conflict(
                    "La lista de materiales cambió. Recarga la página e inténtalo de nuevo.");
        }
        Instant now = clock.instant();
        for (int i = 0; i < orderedIds.size(); i++) {
            Material material = current.get(orderedIds.get(i));
            material.moveTo(i + 1, now);
            materials.save(material);
        }
    }

    private void requireModerate(Actor actor, Topic topic) {
        if (!ContentViews.canModerateLibrary(actor)) {
            courseAccess.requireManageContent(actor, topic.getCourseId());
        }
    }

    private Material requireActive(UUID materialId) {
        return materials.findById(materialId).filter(m -> !m.isArchived())
                .orElseThrow(() -> ApplicationException.notFound("El material no existe."));
    }

    private Topic requireActiveTopic(UUID topicId) {
        return topics.findById(topicId).filter(t -> !t.isArchived())
                .orElseThrow(() -> ApplicationException.notFound("El tema no existe."));
    }

    private static Visibility requireVisibility(Visibility visibility) {
        if (visibility == null) {
            throw ApplicationException.businessRule("Indica si el material es público o privado.");
        }
        return visibility;
    }

    static String validUrl(String raw) {
        String url = Texts.optional(raw);
        if (url == null) {
            throw ApplicationException.businessRule("Escribe la dirección del enlace.");
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (url.length() > MAX_URL_LENGTH || !(scheme.equals("http") || scheme.equals("https"))
                    || uri.getHost() == null) {
                throw ApplicationException.businessRule("El enlace debe ser una dirección web (http:// o https://).");
            }
            return url;
        } catch (URISyntaxException e) {
            throw ApplicationException.businessRule("El enlace no es una dirección web válida.");
        }
    }
}
