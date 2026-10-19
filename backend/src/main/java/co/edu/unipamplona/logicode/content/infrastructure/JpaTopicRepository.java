package co.edu.unipamplona.logicode.content.infrastructure;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.content.domain.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface JpaTopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findByCourseIdAndArchivedAtIsNullOrderByPositionAscCreatedAtAsc(UUID courseId);

    List<Topic> findByArchivedAtIsNullOrderByPositionAscCreatedAtAsc();

    @Query("select coalesce(max(t.position), 0) from Topic t where t.courseId = :courseId")
    int maxPosition(UUID courseId);
}
