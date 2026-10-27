package co.edu.unipamplona.logicode.forum.infrastructure;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.forum.domain.ForumPost;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaForumPostRepository extends JpaRepository<ForumPost, UUID> {

    List<ForumPost> findByThreadIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID threadId);
}
