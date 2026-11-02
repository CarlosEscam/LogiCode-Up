package co.edu.unipamplona.logicode.pseint.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PseintTest {

    private static RunResult run(String code, String input) {
        return Pseint.run(code, input, RunLimits.DEFAULT);
    }

    @Test
    void runsSequenceReadAndWrite() {
        RunResult result = run("""
                Algoritmo Suma
                    Definir a, b Como Entero
                    Escribir "Ingresa dos números:"
                    Leer a, b
                    Escribir "La suma es ", a + b
                    Escribir "Promedio: ", (a + b) / 2
                FinAlgoritmo
                """, "4\n7\n");
        assertThat(result.completed()).isTrue();
        assertThat(result.output()).isEqualTo("Ingresa dos números:\nLa suma es 11\nPromedio: 5.5\n");
    }

    @Test
    void runsConditionalsLoopsAndSwitchInFlexibleSyntax() {
        RunResult result = run("""
                Proceso Ciclos
                    suma = 0;
                    Para i <- 1 Hasta 10 Con Paso 3 Hacer
                        suma <- suma + i
                    Fin Para
                    Escribir Sin Saltar "suma=", suma
                    Escribir ""
                    n <- 0
                    Mientras n < 3
                        n <- n + 1
                    FinMientras
                    Repetir
                        n <- n - 1
                    Hasta Que n = 0
                    Si suma MOD 2 = 0 Y NO (n <> 0) Entonces
                        Escribir "par"
                    SiNo
                        Escribir "impar"
                    FinSi
                    Según 2 Hacer
                        1: Escribir "uno"
                        2, 3:
                            Escribir "dos o tres"
                        De Otro Modo:
                            Escribir "otro"
                    FinSegún
                FinProceso
                """, "");
        assertThat(result.error()).isNull();
        assertThat(result.output()).isEqualTo("suma=22\npar\ndos o tres\n");
    }

    @Test
    void supportsArraysFunctionsAndReferences() {
        RunResult result = run("""
                Funcion total <- Sumar(v, n)
                    total <- 0
                    Para i <- 1 Hasta n
                        total <- total + v[i]
                    FinPara
                FinFuncion

                SubProceso Doblar(x Por Referencia)
                    x <- x * 2
                FinSubProceso

                Algoritmo Arreglos
                    Dimension notas[3]
                    notas[1] <- 3.5; notas[2] <- 4; notas[3] <- 4.5
                    Escribir Sumar(notas, 3)
                    k <- 5
                    Doblar(k)
                    Escribir k, " ", Mayusculas(Subcadena("logicode", 1, 4)), " ", Longitud("hola"), " ", Raiz(16)
                    Escribir 10 > 3, " ", Trunc(-2.7), " ", Redon(2.5), " ", 2 ^ 3, " ", 0.1 + 0.2
                FinAlgoritmo
                """, "");
        assertThat(result.error()).isNull();
        assertThat(result.output()).isEqualTo("12\n10 LOGI 4 4\nVERDADERO -2 3 8 0.3\n");
    }

    @Test
    void reportsSyntaxAndRuntimeErrorsWithTheirLine() {
        RunResult syntax = run("Algoritmo A\n  Si x > 1\n    Escribir 1\nFinAlgoritmo", "");
        assertThat(syntax.errorKind()).isEqualTo(PseintError.Kind.SYNTAX);
        assertThat(syntax.runnable()).isFalse();

        RunResult zero = run("Algoritmo A\n Escribir \"antes\"\n x <- 1 / 0\nFinAlgoritmo", "");
        assertThat(zero.errorKind()).isEqualTo(PseintError.Kind.RUNTIME);
        assertThat(zero.error()).startsWith("Línea 3:").contains("división por cero");
        assertThat(zero.output()).isEqualTo("antes\n");

        RunResult missingInput = run("Algoritmo A\n Leer x\n Leer y\nFinAlgoritmo", "1");
        assertThat(missingInput.error()).contains("faltan datos de entrada");

    }

    @Test
    void runsForEachOverArraysInBothIndexBases() {
        RunResult result = run("""
                Algoritmo Notas
                    Dimension notas[3], cero[2], m[2,2]
                    Para i <- 1 Hasta 3 Hacer
                        Leer notas[i]
                    FinPara
                    suma <- 0
                    Para Cada nota De notas Hacer
                        suma <- suma + nota
                    FinPara
                    Escribir "Promedio: ", suma / 3
                    Para Cada nota de notas
                        nota <- nota * 2
                    Fin Para
                    Escribir notas[1], " ", notas[3]
                    cero[0] <- "a"
                    cero[1] <- "b"
                    Para Cada letra De cero Hacer
                        Escribir Sin Saltar letra
                    FinPara
                    Escribir ""
                    k <- 0
                    Para Cada celda De m Hacer
                        k <- k + 1
                        celda <- k
                    FinPara
                    Escribir m[1,1], m[1,2], m[2,1], m[2,2]
                FinAlgoritmo
                """, "3\n4\n5\n");
        assertThat(result.error()).isNull();
        assertThat(result.output()).isEqualTo("Promedio: 4\n6 10\nab\n1234\n");

        RunResult empty = run("Algoritmo A\n Dimension v[2]\n Para Cada x De v\n  Escribir x\n FinPara\nFinAlgoritmo", "");
        assertThat(empty.errorKind()).isEqualTo(PseintError.Kind.RUNTIME);
        assertThat(empty.error()).contains("«x» no tiene valor");
        assertThat(run("Algoritmo A\n x <- 1\n Para Cada e De x\n FinPara\nFinAlgoritmo", "").error())
                .contains("no es un arreglo");
        assertThat(run("Algoritmo A\n Dimension v[2]\n Para Cada e De v[1]\n FinPara\nFinAlgoritmo", "").errorKind())
                .isEqualTo(PseintError.Kind.SYNTAX);
        assertThat(run("Algoritmo A\n Dimension v[2]\n Para Cada e De v\nFinAlgoritmo", "").errorKind())
                .isEqualTo(PseintError.Kind.SYNTAX);
    }

    @Test
    void stopsInfiniteLoopsRecursionHugeArraysAndOutput() {
        assertThat(run("Algoritmo A\n Mientras Verdadero Hacer\n FinMientras\nFinAlgoritmo", "").errorKind())
                .isEqualTo(PseintError.Kind.LIMIT);
        assertThat(run("Funcion r <- F(n)\n r <- F(n + 1)\nFinFuncion\nAlgoritmo A\n Escribir F(1)\nFinAlgoritmo", "")
                .errorKind()).isEqualTo(PseintError.Kind.LIMIT);
        assertThat(run("Algoritmo A\n Dimension v[100000]\nFinAlgoritmo", "").errorKind())
                .isEqualTo(PseintError.Kind.LIMIT);
        RunResult flood = run("Algoritmo A\n Repetir\n  Escribir \"xxxxxxxxxx\"\n Hasta Que Falso\nFinAlgoritmo", "");
        assertThat(flood.errorKind()).isEqualTo(PseintError.Kind.LIMIT);
        assertThat(flood.output()).hasSize(RunLimits.DEFAULT.maxOutputChars());
        assertThat(run("Algoritmo A\n t <- \"x\"\n Mientras Verdadero\n  t <- t + t\n FinMientras\nFinAlgoritmo", "")
                .error()).contains("texto superó");
        StringBuilder nested = new StringBuilder("Algoritmo A\n x <- ");
        nested.append("(".repeat(500)).append('1').append(")".repeat(500)).append("\nFinAlgoritmo");
        assertThat(run(nested.toString(), "").errorKind()).isEqualTo(PseintError.Kind.LIMIT);
    }

    @Test
    void testCasesIgnorePromptsButNotWrongValues() {
        String code = """
                Algoritmo Par
                    Escribir "Escribe un número:"
                    Leer n
                    Si n MOD 2 = 0 Entonces
                        Escribir n, " es par"
                    SiNo
                        Escribir n, " es impar"
                    FinSi
                FinAlgoritmo
                """;
        TestCases.Report report = TestCases.check(code, List.of(
                new TestCases.TestCase("4", "4 es par"),
                new TestCases.TestCase("7", "7 es impar"),
                new TestCases.TestCase("9", "9 es par")), RunLimits.DEFAULT);
        assertThat(report.runnable()).isTrue();
        assertThat(report.passed()).isEqualTo(2);
        assertThat(report.score()).isEqualByComparingTo("3.3");
        assertThat(report.cases().get(2).passed()).isFalse();

        assertThat(TestCases.matches("15", "Resultado: 15\n")).isTrue();
        assertThat(TestCases.matches("15", "Resultado: 150\n")).isFalse();
        assertThat(TestCases.matches("5", "-5")).isFalse();
        assertThat(TestCases.matches("5", "5.5")).isFalse();
        assertThat(TestCases.matches("Hola\nMundo", "hola   MUNDO")).isTrue();
        assertThat(TestCases.matches("Mundo\nHola", "hola mundo")).isFalse();

        TestCases.Report broken = TestCases.check("Algoritmo A\n Escribir (\nFinAlgoritmo", List.of(
                new TestCases.TestCase("", "1")), RunLimits.DEFAULT);
        assertThat(broken.runnable()).isFalse();
        assertThat(broken.score()).isEqualByComparingTo("0.0");
    }
}
