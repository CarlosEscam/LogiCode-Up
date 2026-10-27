package co.edu.unipamplona.logicode.forum.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.forum.application.ForumPostRepository;
import co.edu.unipamplona.logicode.forum.domain.ForumPost;
import org.springframework.stereotype.Repository;

@Repository
class ForumPostRepositoryAdapter implements ForumPostRepository {

    private final JpaForumPostRepository jpa;

    ForumPostRepositoryAdapter(JpaForumPostRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ForumPost save(ForumPost post) {
        return jpa.save(post);
    }

    @Override
    public Optional<ForumPost> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<ForumPost> findVisibleByThread(UUID threadId) {
        return jpa.findByThreadIdAndDeletedAtIsNullOrderByCreatedAtAsc(threadId);
    }
}
