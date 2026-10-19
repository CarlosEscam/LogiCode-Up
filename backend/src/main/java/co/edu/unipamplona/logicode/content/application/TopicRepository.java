package co.edu.unipamplona.logicode.content.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.content.domain.Topic;

public interface TopicRepository {

    Topic save(Topic topic);

    /** Guarda y envía el cambio a la base de inmediato (p. ej. antes de marcar otra próxima clase). */
    Topic saveAndFlush(Topic topic);

    Optional<Topic> findById(UUID id);

    /** Temas no archivados de un curso, en su orden. */
    List<Topic> findActiveByCourse(UUID courseId);

    /** Temas no archivados de todos los cursos. */
    List<Topic> findAllActive();

    int nextPosition(UUID courseId);
}
