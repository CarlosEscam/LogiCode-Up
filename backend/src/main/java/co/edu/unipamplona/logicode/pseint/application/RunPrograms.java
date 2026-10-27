package co.edu.unipamplona.logicode.pseint.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import co.edu.unipamplona.logicode.pseint.domain.Pseint;
import co.edu.unipamplona.logicode.pseint.domain.RunLimits;
import co.edu.unipamplona.logicode.pseint.domain.RunResult;
import co.edu.unipamplona.logicode.pseint.domain.TestCases;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;

/**
 * Ejecuta programas PSeInt con los límites de {@link RunLimits#DEFAULT} (RNF-05): para el editor y para calificar
 * con casos de prueba (RF-16). A lo sumo {@value #CONCURRENT_RUNS} ejecuciones a la vez, para que muchos estudiantes
 * probando código no dejen sin CPU al resto de la plataforma.
 */
@Service
public class RunPrograms {

    public record ProgramRun(String output, boolean completed, String errorKind, String error, long steps,
            long durationMs) {
    }

    public record TestCase(String input, String expectedOutput) {
    }

    public record CaseOutcome(int number, String input, String expectedOutput, String output, boolean passed,
            String error) {
    }

    /** {@code programError}: el programa no se pudo leer; no hay resultados por caso. */
    public record TestReport(List<CaseOutcome> cases, String programError, int passed, int total, BigDecimal score) {

        public boolean runnable() {
            return programError == null;
        }
    }

    static final int CONCURRENT_RUNS = 4;
    private static final long WAIT_SECONDS = 10;

    private final Semaphore slots = new Semaphore(CONCURRENT_RUNS, true);

    public ProgramRun run(String code, String input) {
        return withSlot(() -> {
            long started = System.nanoTime();
            RunResult result = Pseint.run(code, input, RunLimits.DEFAULT);
            return new ProgramRun(result.output(), result.completed(),
                    result.errorKind() == null ? null : result.errorKind().name(), result.error(), result.steps(),
                    (System.nanoTime() - started) / 1_000_000);
        });
    }

    public TestReport check(String code, List<TestCase> cases) {
        return withSlot(() -> {
            TestCases.Report report = TestCases.check(code, cases.stream()
                    .map(c -> new TestCases.TestCase(c.input(), c.expectedOutput())).toList(), RunLimits.DEFAULT);
            return new TestReport(report.cases().stream().map(c -> new CaseOutcome(c.number(), c.input(),
                    c.expectedOutput(), c.output(), c.passed(), c.error())).toList(), report.programError(),
                    report.passed(), report.total(), report.score());
        });
    }

    private <T> T withSlot(Supplier<T> work) {
        boolean acquired;
        try {
            acquired = slots.tryAcquire(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            acquired = false;
        }
        if (!acquired) {
            throw ApplicationException.tooManyRequests("El intérprete está ocupado. Intenta de nuevo en unos segundos.");
        }
        try {
            return work.get();
        } finally {
            slots.release();
        }
    }
}
