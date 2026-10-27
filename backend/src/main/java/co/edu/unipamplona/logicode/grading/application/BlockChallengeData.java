package co.edu.unipamplona.logicode.grading.application;

import java.util.List;

import co.edu.unipamplona.logicode.grading.domain.BlockChallenge;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reto de bloques (RF-12) tal como viaja en JSON: en la rúbrica que define el docente, en la columna
 * {@code rubric_version.block_challenge} y, sus bloques, en la respuesta escrita de la entrega. Es el mismo formato de
 * programa que usa el juego de la interfaz ({@code type}, {@code times}, {@code body}, {@code else}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BlockChallengeData(List<String> grid, String facing, Integer maxBlocks, List<String> allowed,
        List<BlockData> start) {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record BlockData(String type, Integer times, List<BlockData> body,
            @JsonProperty("else") List<BlockData> orElse) {
    }

    // La interfaz manda también el id de cada bloque, que aquí no se usa.
    private static final JsonMapper JSON = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    private static final TypeReference<List<BlockData>> PROGRAM = new TypeReference<>() {
    };

    /** Valida el reto; los errores vienen en {@link IllegalArgumentException} con un mensaje para el docente. */
    BlockChallenge toDomain() {
        return new BlockChallenge(grid, facing, maxBlocks == null ? 0 : maxBlocks, allowed, blocks(start));
    }

    static BlockChallengeData of(BlockChallenge challenge) {
        return new BlockChallengeData(challenge.getGrid(), challenge.getFacing(), challenge.getMaxBlocks(),
                challenge.getAllowed(), data(challenge.getStart()));
    }

    static String toJson(BlockChallenge challenge) {
        return JSON.writeValueAsString(of(challenge));
    }

    /** Lo guardado en la columna, que ya pasó la validación al definir la rúbrica. */
    static BlockChallengeData read(String json) {
        return JSON.readValue(json, BlockChallengeData.class);
    }

    /** Programa entregado por el estudiante; JSON inválido o bloques sin tipo dan {@link IllegalArgumentException}. */
    static List<BlockChallenge.Block> program(String json) {
        try {
            return blocks(JSON.readValue(json, PROGRAM));
        } catch (JacksonException e) {
            throw new IllegalArgumentException("La respuesta no es un programa de bloques.", e);
        }
    }

    private static List<BlockChallenge.Block> blocks(List<BlockData> data) {
        if (data == null) {
            return List.of();
        }
        return data.stream().map(b -> {
            if (b == null || b.type() == null) {
                throw new IllegalArgumentException("Hay un bloque sin tipo.");
            }
            return new BlockChallenge.Block(b.type(), b.times() == null ? 0 : b.times(), blocks(b.body()),
                    blocks(b.orElse()));
        }).toList();
    }

    private static List<BlockData> data(List<BlockChallenge.Block> blocks) {
        return blocks.stream().map(b -> new BlockData(b.type(), b.type().equals("repetir") ? b.times() : null,
                data(b.body()), data(b.orElse()))).toList();
    }
}
