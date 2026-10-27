package co.edu.unipamplona.logicode.forum.infrastructure;

import java.util.UUID;

import co.edu.unipamplona.logicode.forum.domain.ForumThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface JpaForumThreadRepository extends JpaRepository<ForumThread, UUID>, JpaSpecificationExecutor<ForumThread> {
}
