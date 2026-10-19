package co.edu.unipamplona.logicode.content.application;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.content.domain.Tool;

/** Tema de un curso con sus materiales en orden. */
public record TopicView(UUID id, UUID courseId, String title, String description, Tool tool, int position,
        boolean nextClass, List<MaterialView> materials) {
}
