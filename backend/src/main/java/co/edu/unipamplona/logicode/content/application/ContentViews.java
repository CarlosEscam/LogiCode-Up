package co.edu.unipamplona.logicode.content.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.edu.unipamplona.logicode.content.domain.Material;
import co.edu.unipamplona.logicode.content.domain.Topic;
import co.edu.unipamplona.logicode.courses.application.CourseAccess;
import co.edu.unipamplona.logicode.files.application.FileStore;
import co.edu.unipamplona.logicode.files.application.StoredFileInfo;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.domain.Permission;
import org.springframework.stereotype.Component;

/** Arma las vistas de temas y materiales, con los permisos del actor y los datos de cada archivo. */
@Component
class ContentViews {

    private final CourseAccess courseAccess;
    private final FileStore fileStore;

    ContentViews(CourseAccess courseAccess, FileStore fileStore) {
        this.courseAccess = courseAccess;
        this.fileStore = fileStore;
    }

    List<MaterialView> materials(Actor actor, List<Material> materials, Map<UUID, Topic> topics) {
        Map<UUID, StoredFileInfo> files = fileStore.describe(materials.stream().map(Material::getFileId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<UUID, String> courseNames = new HashMap<>();
        Map<UUID, Boolean> manageable = new HashMap<>();
        boolean moderator = canModerateLibrary(actor);
        return materials.stream().map(m -> {
            Topic topic = topics.get(m.getTopicId());
            UUID courseId = topic.getCourseId();
            String courseName = courseNames.computeIfAbsent(courseId, id -> courseAccess.summary(id).name());
            boolean canEdit = manageable.computeIfAbsent(courseId, id -> courseAccess.canManageContent(actor, id));
            return new MaterialView(m.getId(), topic.getId(), topic.getTitle(), topic.getTool(), courseId,
                    courseName, m.getTitle(), m.getDescription(), m.getKind(), m.getVisibility(), m.getPosition(),
                    m.getExternalUrl(), m.getFileId() == null ? null : files.get(m.getFileId()), m.getCreatedAt(),
                    m.getUpdatedAt(), canEdit, canEdit || moderator);
        }).toList();
    }

    MaterialView material(Actor actor, Material material, Topic topic) {
        return materials(actor, List.of(material), Map.of(topic.getId(), topic)).getFirst();
    }

    List<TopicView> topics(Actor actor, List<Topic> topics, List<Material> materials) {
        Map<UUID, Topic> byId = topics.stream().collect(Collectors.toMap(Topic::getId, Function.identity()));
        Map<UUID, List<MaterialView>> byTopic = materials(actor, materials, byId).stream()
                .collect(Collectors.groupingBy(MaterialView::topicId));
        return topics.stream().map(t -> new TopicView(t.getId(), t.getCourseId(), t.getTitle(), t.getDescription(),
                t.getTool(), t.getPosition(), t.isNextClass(), byTopic.getOrDefault(t.getId(), List.of()))).toList();
    }

    static boolean canModerateLibrary(Actor actor) {
        return actor != null && actor.can(Permission.MANAGE_LIBRARY);
    }
}
