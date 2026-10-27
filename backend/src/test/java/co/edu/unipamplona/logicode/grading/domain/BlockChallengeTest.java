package co.edu.unipamplona.logicode.grading.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import co.edu.unipamplona.logicode.grading.domain.BlockChallenge.Block;
import co.edu.unipamplona.logicode.grading.domain.BlockChallenge.Outcome;
import org.junit.jupiter.api.Test;

class BlockChallengeTest {

    static final List<String> ALL = List.of("avanzar", "izquierda", "derecha", "repetir", "si", "mientras");

    static Block b(String type) {
        return new Block(type, 0, null, null);
    }

    @Test
    void reachesTheGoalWithTheSameRulesAsTheGame() {
        // Nivel "Sigue el pasillo" del juego: mientras no llegue, si hay camino avanza, si no gira a la derecha.
        BlockChallenge corridor = new BlockChallenge(List.of("S....", "####.", "#M##.", "#...."), "E", 4, ALL, null);
        Block loop = new Block("mientras", 0, List.of(new Block("si", 0, List.of(b("avanzar")),
                List.of(b("derecha")))), null);
        assertThat(corridor.run(List.of(loop))).isEqualTo(new BlockChallenge.Result(Outcome.GOAL, 4));

        BlockChallenge hall = new BlockChallenge(List.of("S.........M"), "E", 2, ALL, null);
        assertThat(hall.run(List.of(new Block("repetir", 10, List.of(b("avanzar")), null))).score())
                .isEqualByComparingTo("5.0");
    }

    @Test
    void reportsCrashesProgramsThatStopShortAndEndlessLoops() {
        BlockChallenge hall = new BlockChallenge(List.of("S.#M"), "E", 10, ALL, null);
        assertThat(hall.run(List.of(b("avanzar"), b("avanzar"))).outcome()).isEqualTo(Outcome.CRASH);
        assertThat(hall.run(List.of(b("avanzar"))).outcome()).isEqualTo(Outcome.ENDED);
        assertThat(hall.run(List.of(new Block("mientras", 0, List.of(b("derecha")), null))).outcome())
                .isEqualTo(Outcome.LIMIT);
        assertThat(hall.run(List.of(b("avanzar"))).score()).isEqualByComparingTo("0.0");
    }

    @Test
    void rejectsBadBoardsAndProgramsThatBreakTheRules() {
        assertThatThrownBy(() -> new BlockChallenge(List.of("S..", "M"), "E", 3, ALL, null))
                .hasMessageContaining("mismo largo");
        assertThatThrownBy(() -> new BlockChallenge(List.of("S.SM"), "E", 3, ALL, null))
                .hasMessageContaining("una salida");
        assertThatThrownBy(() -> new BlockChallenge(List.of("S.XM"), "E", 3, ALL, null))
                .hasMessageContaining("solo admite");
        assertThatThrownBy(() -> new BlockChallenge(List.of("S.M"), "E", 3, List.of("volar"), null))
                .hasMessageContaining("bloque");

        BlockChallenge onlySteps = new BlockChallenge(List.of("S..M"), "E", 2, List.of("avanzar"), null);
        assertThatThrownBy(() -> onlySteps.run(List.of(b("avanzar"), b("avanzar"), b("avanzar"))))
                .hasMessageContaining("más de 2 bloques");
        assertThatThrownBy(() -> onlySteps.run(List.of(new Block("repetir", 3, List.of(b("avanzar")), null))))
                .hasMessageContaining("no permite");
        assertThatThrownBy(() -> new BlockChallenge(List.of("S..M"), "E", 2, List.of("avanzar"),
                List.of(b("derecha")))).hasMessageContaining("programa inicial");
    }
}
