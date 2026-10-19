package co.edu.unipamplona.logicode.content.application;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.edu.unipamplona.logicode.content.domain.Tool;
import co.edu.unipamplona.logicode.content.domain.Topic;
import co.edu.unipamplona.logicode.courses.application.CourseAccess;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF-06 y RF-09: el docente del curso crea, edita, ordena y archiva temas, y marca el de la próxima clase. */
@Service
public class ManageTopics {

    private final TopicRepository topics;
    private final CourseAccess courseAccess;
    private final ContentViews views;
    private final Clock clock;

    public ManageTopics(TopicRepository topics, CourseAccess courseAccess, ContentViews views, Clock clock) {
        this.topics = topics;
        this.courseAccess = courseAccess;
        this.views = views;
        this.clock = clock;
    }

    @Transactional
    public TopicView create(Actor actor, UUID courseId, String title, String description, Tool tool) {
        courseAccess.requireManageContent(actor, courseId);
        Topic topic = topics.save(new Topic(courseId, Texts.required(title, "El título del tema es obligatorio."),
                Texts.optional(description), requireTool(tool), topics.nextPosition(courseId), clock.instant()));
        return view(actor, topic);
    }

    @Transactional
    public TopicView update(Actor actor, UUID topicId, String title, String description, Tool tool) {
        Topic topic = requireManageable(actor, topicId);
        topic.update(Texts.required(title, "El título del tema es obligatorio."), Texts.optional(description),
                requireTool(tool), clock.instant());
        return view(actor, topics.save(topic));
    }

    /** Recibe todos los temas del curso en el nuevo orden. */
    @Transactional
    public void reorder(Actor actor, UUID courseId, List<UUID> orderedIds) {
        courseAccess.requireManageContent(actor, courseId);
        Map<UUID, Topic> current = topics.findActiveByCourse(courseId).stream()
                .collect(Collectors.toMap(Topic::getId, Function.identity()));
        if (orderedIds == null || orderedIds.size() != current.size()
                || !new HashSet<>(orderedIds).equals(current.keySet())) {
            throw ApplicationException.conflict("La lista de temas cambió. Recarga la página e inténtalo de nuevo.");
        }
        Instant now = clock.instant();
        for (int i = 0; i < orderedIds.size(); i++) {
            Topic topic = current.get(orderedIds.get(i));
            topic.moveTo(i + 1, now);
            topics.save(topic);
        }
    }

    /** Marca el tema de la próxima clase del curso; {@code topicId} nulo quita la marca. */
    @Transactional
    public void setNextClass(Actor actor, UUID courseId, UUID topicId) {
        courseAccess.requireManageContent(actor, courseId);
        Instant now = clock.instant();
        Topic target = null;
        if (topicId != null) {
            target = topics.findById(topicId).filter(t -> t.getCourseId().equals(courseId) && !t.isArchived())
                    .orElseThrow(() -> ApplicationException.notFound("El tema no existe."));
        }
        for (Topic topic : topics.findActiveByCourse(courseId)) {
            if (topic.isNextClass() && topic != target) {
                topic.markNextClass(false, now);
                topics.saveAndFlush(topic);
            }
        }
        if (target != null) {
            target.markNextClass(true, now);
            topics.save(target);
        }
    }

    /** Archiva el tema: deja de mostrarse con sus materiales; los archivos se conservan (RN-21). */
    @Transactional
    public void archive(Actor actor, UUID topicId) {
        Topic topic = requireManageable(actor, topicId);
        topic.archive(clock.instant());
        topics.save(topic);
    }

    private Topic requireManageable(Actor actor, UUID topicId) {
        Topic topic = topics.findById(topicId).filter(t -> !t.isArchived())
                .orElseThrow(() -> ApplicationException.notFound("El tema no existe."));
        courseAccess.requireManageContent(actor, topic.getCourseId());
        return topic;
    }

    private TopicView view(Actor actor, Topic topic) {
        return views.topics(actor, List.of(topic), List.of()).getFirst();
    }

    private static Tool requireTool(Tool tool) {
        if (tool == null) {
            throw ApplicationException.businessRule("Elige la herramienta del tema.");
        }
        return tool;
    }
}
