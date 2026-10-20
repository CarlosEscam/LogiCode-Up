package co.edu.unipamplona.logicode.activities.domain;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda los grupos de formatos como texto separado por comas (p. ej. {@code DOCUMENTS,IMAGES}). */
@Converter
class FileGroupsConverter implements AttributeConverter<Set<FileGroup>, String> {

    @Override
    public String convertToDatabaseColumn(Set<FileGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return null;
        }
        return groups.stream().sorted().map(Enum::name).collect(Collectors.joining(","));
    }

    @Override
    public Set<FileGroup> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return EnumSet.noneOf(FileGroup.class);
        }
        return Arrays.stream(value.split(",")).map(String::trim).map(FileGroup::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FileGroup.class)));
    }
}
