package co.edu.unipamplona.logicode.content.infrastructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import co.edu.unipamplona.logicode.content.application.MaterialRepository;
import co.edu.unipamplona.logicode.content.domain.Material;
import co.edu.unipamplona.logicode.content.domain.Visibility;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class MaterialRepositoryAdapter implements MaterialRepository {

    private final JpaMaterialRepository jpa;

    MaterialRepositoryAdapter(JpaMaterialRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Material save(Material material) {
        return jpa.save(material);
    }

    @Override
    public Optional<Material> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<Material> findActiveByTopics(Collection<UUID> topicIds) {
        return topicIds.isEmpty() ? List.of()
                : jpa.findByTopicIdInAndArchivedAtIsNullOrderByPositionAscCreatedAtAsc(topicIds);
    }

    @Override
    public int nextPosition(UUID topicId) {
        return jpa.maxPosition(topicId) + 1;
    }

    @Override
    public PageResult<Material> search(Search search, int page, int size) {
        Page<Material> result = jpa.findAll(visibleIn(search),
                PageRequest.of(page, size, Sort.by("updatedAt").descending().and(Sort.by("id"))));
        return new PageResult<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    @Override
    public Set<UUID> topicsWithMaterials(Search search) {
        Set<UUID> topics = new HashSet<>();
        jpa.findAll(visibleIn(search)).forEach(m -> topics.add(m.getTopicId()));
        return topics;
    }

    /** Traduce la búsqueda a condiciones SQL; los conjuntos vacíos se omiten para no generar {@code IN ()}. */
    private static Specification<Material> visibleIn(Search search) {
        return (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.isNull(root.get("archivedAt")));
            conditions.add(search.topicIds().isEmpty() ? cb.disjunction() : root.get("topicId").in(search.topicIds()));
            Predicate isPublic = cb.equal(root.get("visibility"), Visibility.PUBLIC);
            conditions.add(search.privateTopicIds().isEmpty() ? isPublic
                    : cb.or(isPublic, root.get("topicId").in(search.privateTopicIds())));
            if (search.kind() != null) {
                conditions.add(cb.equal(root.get("kind"), search.kind()));
            }
            if (search.text() != null) {
                String pattern = "%" + escape(search.text().toLowerCase(Locale.ROOT)) + "%";
                Predicate inTitle = cb.like(cb.lower(root.get("title")), pattern, '\\');
                Predicate inDescription = cb.like(cb.lower(root.get("description")), pattern, '\\');
                conditions.add(search.textTopicIds().isEmpty() ? cb.or(inTitle, inDescription)
                        : cb.or(inTitle, inDescription, root.get("topicId").in(search.textTopicIds())));
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
