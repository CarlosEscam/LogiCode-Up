package co.edu.unipamplona.logicode.content.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.content.application.TopicRepository;
import co.edu.unipamplona.logicode.content.domain.Topic;
import org.springframework.stereotype.Repository;

@Repository
class TopicRepositoryAdapter implements TopicRepository {

    private final JpaTopicRepository jpa;

    TopicRepositoryAdapter(JpaTopicRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Topic save(Topic topic) {
        return jpa.save(topic);
    }

    @Override
    public Topic saveAndFlush(Topic topic) {
        return jpa.saveAndFlush(topic);
    }

    @Override
    public Optional<Topic> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<Topic> findActiveByCourse(UUID courseId) {
        return jpa.findByCourseIdAndArchivedAtIsNullOrderByPositionAscCreatedAtAsc(courseId);
    }

    @Override
    public List<Topic> findAllActive() {
        return jpa.findByArchivedAtIsNullOrderByPositionAscCreatedAtAsc();
    }

    @Override
    public int nextPosition(UUID courseId) {
        return jpa.maxPosition(courseId) + 1;
    }
}
