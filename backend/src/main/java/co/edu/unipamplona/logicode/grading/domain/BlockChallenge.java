package co.edu.unipamplona.logicode.grading.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Reto de bloques que configura el docente (RF-12, RF-22): un robot en una cuadrícula ('#' muro, '.' libre, 'S'
 * salida, 'M' meta) y los bloques que se pueden usar. Mismas reglas que el intérprete del juego en la interfaz
 * (frontend/src/features/games/blocks.js); aquí se vuelve a ejecutar el programa entregado para calificarlo.
 */
public final class BlockChallenge {

    public static final Set<String> TYPES = Set.of("avanzar", "izquierda", "derecha", "repetir", "si", "mientras");
    public static final int MAX_SIDE = 12;
    public static final int MAX_BLOCKS = 40;
    public static final int MAX_STEPS = 400;
    private static final List<String> DIRECTIONS = List.of("N", "E", "S", "O");
    private static final int[][] DELTA = { { -1, 0 }, { 0, 1 }, { 1, 0 }, { 0, -1 } };

    /** Un bloque. {@code times} solo cuenta en repetir (2 a 12); {@code body} y {@code orElse} en los contenedores. */
    public record Block(String type, int times, List<Block> body, List<Block> orElse) {

        public Block {
            body = body == null ? List.of() : List.copyOf(body);
            orElse = orElse == null ? List.of() : List.copyOf(orElse);
        }
    }

    public enum Outcome {
        GOAL, CRASH, ENDED, LIMIT
    }

    public record Result(Outcome outcome, int blocks) {

        /** Llegar a la meta es 5,0; no llegar, 0,0. */
        public BigDecimal score() {
            return outcome == Outcome.GOAL ? Grade.MAX_SCORE : new BigDecimal("0.0");
        }
    }

    private final List<String> grid;
    private final String facing;
    private final int maxBlocks;
    private final List<String> allowed;
    private final List<Block> start;
    private final int startRow;
    private final int startCol;
    private final int goalRow;
    private final int goalCol;

    public BlockChallenge(List<String> grid, String facing, int maxBlocks, List<String> allowed, List<Block> start) {
        if (grid == null || grid.isEmpty() || grid.size() > MAX_SIDE) {
            throw new IllegalArgumentException("El tablero debe tener entre 1 y " + MAX_SIDE + " filas.");
        }
        int width = grid.get(0).length();
        int starts = 0;
        int goals = 0;
        int sr = 0, sc = 0, gr = 0, gc = 0;
        for (int r = 0; r < grid.size(); r++) {
            String row = grid.get(r);
            if (row.length() != width || width == 0 || width > MAX_SIDE) {
                throw new IllegalArgumentException("Todas las filas del tablero deben tener entre 1 y " + MAX_SIDE
                        + " casillas y el mismo largo.");
            }
            for (int c = 0; c < width; c++) {
                switch (row.charAt(c)) {
                    case 'S' -> { starts++; sr = r; sc = c; }
                    case 'M' -> { goals++; gr = r; gc = c; }
                    case '#', '.' -> { }
                    default -> throw new IllegalArgumentException(
                            "El tablero solo admite # (muro), . (libre), S (salida) y M (meta).");
                }
            }
        }
        if (starts != 1 || goals != 1) {
            throw new IllegalArgumentException("El tablero necesita exactamente una salida (S) y una meta (M).");
        }
        if (!DIRECTIONS.contains(facing)) {
            throw new IllegalArgumentException("El robot debe empezar mirando a N, E, S u O.");
        }
        if (maxBlocks < 1 || maxBlocks > MAX_BLOCKS) {
            throw new IllegalArgumentException("El máximo de bloques va de 1 a " + MAX_BLOCKS + ".");
        }
        if (allowed == null || allowed.isEmpty() || !TYPES.containsAll(allowed)) {
            throw new IllegalArgumentException("Elige por lo menos un bloque de la lista.");
        }
        this.grid = List.copyOf(grid);
        this.facing = facing;
        this.maxBlocks = maxBlocks;
        this.allowed = List.copyOf(allowed);
        this.start = start == null ? List.of() : List.copyOf(start);
        this.startRow = sr;
        this.startCol = sc;
        this.goalRow = gr;
        this.goalCol = gc;
        check(this.start, "El programa inicial");
    }

    /**
     * Ejecuta el programa entregado. Un programa con bloques no permitidos o con más bloques de los permitidos no se
     * ejecuta: lanza {@link IllegalArgumentException}.
     */
    public Result run(List<Block> program) {
        check(program, "El programa");
        Robot robot = new Robot();
        Outcome outcome;
        try {
            robot.run(program);
            outcome = robot.atGoal() ? Outcome.GOAL : Outcome.ENDED;
        } catch (Stop stop) {
            outcome = stop.outcome;
        }
        return new Result(outcome, count(program));
    }

    private void check(List<Block> program, String what) {
        if (count(program) > maxBlocks) {
            throw new IllegalArgumentException(what + " usa más de " + maxBlocks + " bloques.");
        }
        checkTypes(program, what);
    }

    private void checkTypes(List<Block> blocks, String what) {
        for (Block block : blocks) {
            if (!allowed.contains(block.type())) {
                throw new IllegalArgumentException(what + " usa un bloque que el reto no permite: " + block.type());
            }
            if (block.type().equals("repetir") && (block.times() < 2 || block.times() > 12)) {
                throw new IllegalArgumentException("Repetir va de 2 a 12 veces.");
            }
            checkTypes(block.body(), what);
            checkTypes(block.orElse(), what);
        }
    }

    public static int count(List<Block> blocks) {
        return blocks.stream().mapToInt(b -> 1 + count(b.body()) + count(b.orElse())).sum();
    }

    private static final class Stop extends RuntimeException {
        private final Outcome outcome;

        Stop(Outcome outcome) {
            super(null, null, false, false);
            this.outcome = outcome;
        }
    }

    private final class Robot {
        int r = startRow;
        int c = startCol;
        int dir = DIRECTIONS.indexOf(facing);
        int steps;

        boolean atGoal() {
            return r == goalRow && c == goalCol;
        }

        boolean free(int row, int col) {
            return row >= 0 && row < grid.size() && col >= 0 && col < grid.get(row).length()
                    && grid.get(row).charAt(col) != '#';
        }

        boolean aheadFree() {
            return free(r + DELTA[dir][0], c + DELTA[dir][1]);
        }

        void tick() {
            if (++steps > MAX_STEPS) {
                throw new Stop(Outcome.LIMIT);
            }
        }

        void run(List<Block> blocks) {
            for (Block block : blocks) {
                tick();
                switch (block.type()) {
                    case "avanzar" -> {
                        if (!aheadFree()) {
                            throw new Stop(Outcome.CRASH);
                        }
                        r += DELTA[dir][0];
                        c += DELTA[dir][1];
                        if (atGoal()) {
                            throw new Stop(Outcome.GOAL);
                        }
                    }
                    case "izquierda" -> dir = (dir + 3) % 4;
                    case "derecha" -> dir = (dir + 1) % 4;
                    case "repetir" -> {
                        for (int i = 0; i < block.times(); i++) {
                            run(block.body());
                        }
                    }
                    case "si" -> run(aheadFree() ? block.body() : block.orElse());
                    case "mientras" -> {
                        while (!atGoal()) {
                            tick();
                            run(block.body());
                        }
                    }
                    default -> throw new IllegalArgumentException("Bloque desconocido: " + block.type());
                }
            }
        }
    }

    public List<String> getGrid() {
        return grid;
    }

    public String getFacing() {
        return facing;
    }

    public int getMaxBlocks() {
        return maxBlocks;
    }

    public List<String> getAllowed() {
        return allowed;
    }

    public List<Block> getStart() {
        return start;
    }
}
