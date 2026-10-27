package co.edu.unipamplona.logicode.forum.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.forum.application.ForumThreadRepository;
import co.edu.unipamplona.logicode.forum.domain.ForumCategory;
import co.edu.unipamplona.logicode.forum.domain.ForumThread;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class ForumThreadRepositoryAdapter implements ForumThreadRepository {

    private final JpaForumThreadRepository jpa;

    ForumThreadRepositoryAdapter(JpaForumThreadRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ForumThread save(ForumThread thread) {
        return jpa.save(thread);
    }

    @Override
    public Optional<ForumThread> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public PageResult<ForumThread> search(ForumCategory category, String text, int page, int size) {
        Specification<ForumThread> filter = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.isNull(root.get("deletedAt")));
            if (category != null) {
                conditions.add(cb.equal(root.get("category"), category));
            }
            if (text != null) {
                String pattern = "%" + escape(text.toLowerCase(Locale.ROOT)) + "%";
                conditions.add(cb.or(cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("body")), pattern, '\\')));
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
        Page<ForumThread> result = jpa.findAll(filter, PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("lastActivityAt"), Sort.Order.asc("id"))));
        return new PageResult<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
