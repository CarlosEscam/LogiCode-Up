package co.edu.unipamplona.logicode.quizzes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import co.edu.unipamplona.logicode.quizzes.application.ManageQuestionBank.OptionInput;
import co.edu.unipamplona.logicode.quizzes.application.ManageQuestionBank.QuestionInput;
import co.edu.unipamplona.logicode.quizzes.domain.QuestionKind;
import co.edu.unipamplona.logicode.quizzes.domain.QuizOption;
import co.edu.unipamplona.logicode.quizzes.domain.QuizQuestion;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Pruebas de caracterización de la validación de preguntas (guía 15, deuda D-15): fijan lo que hace hoy, mensaje por
 * mensaje, antes de partir el método en partes más pequeñas. Se escribieron y pasaron sobre el código anterior.
 */
class ManageQuestionBankValidationTest {

    private static final List<OptionInput> TWO = List.of(new OptionInput("A", true), new OptionInput("B", false));

    private static QuestionInput q(QuestionKind kind, List<OptionInput> options, List<String> answers) {
        return new QuestionInput(kind, "¿Cuánto es 2 + 2?", 3, null, options, answers);
    }

    private static void rejects(QuestionInput input, String message) {
        assertThatThrownBy(() -> ManageQuestionBank.validate(input))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(message);
    }

    private static List<OptionInput> options(int n) {
        return IntStream.range(0, n).mapToObj(i -> new OptionInput("Opción " + i, i == 0)).toList();
    }

    @Test
    void commonFields() {
        rejects(new QuestionInput(null, "x", 1, null, TWO, null), "Elige el tipo de pregunta.");
        rejects(new QuestionInput(QuestionKind.SINGLE, "   ", 1, null, TWO, null),
                "Escribe el enunciado de la pregunta (hasta 2000 caracteres).");
        rejects(new QuestionInput(QuestionKind.SINGLE, null, 1, null, TWO, null),
                "Escribe el enunciado de la pregunta (hasta 2000 caracteres).");
        rejects(new QuestionInput(QuestionKind.SINGLE, "a".repeat(2001), 1, null, TWO, null),
                "Escribe el enunciado de la pregunta (hasta 2000 caracteres).");
        rejects(new QuestionInput(QuestionKind.SINGLE, "x", 0, null, TWO, null), "Los puntos de la pregunta van de 1 a 10.");
        rejects(new QuestionInput(QuestionKind.SINGLE, "x", 11, null, TWO, null), "Los puntos de la pregunta van de 1 a 10.");
        rejects(new QuestionInput(QuestionKind.SINGLE, "x", 1, "e".repeat(2001), TWO, null),
                "La explicación admite hasta 2000 caracteres.");
    }

    @Test
    void cleansTextAndDefaultsToOnePoint() {
        QuizQuestion.Definition d = ManageQuestionBank.validate(
                new QuestionInput(QuestionKind.SINGLE, "  Enunciado  ", null, "   ", List.of(
                        new OptionInput("  Sí ", true), new OptionInput("No", false)), List.of("ignorada")));
        assertThat(d.kind()).isEqualTo(QuestionKind.SINGLE);
        assertThat(d.prompt()).isEqualTo("Enunciado");
        assertThat(d.points()).isEqualTo(1);
        assertThat(d.explanation()).isNull();
        assertThat(d.options()).extracting(QuizOption::getText, QuizOption::isCorrect)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Sí", true), org.assertj.core.groups.Tuple.tuple("No", false));
        assertThat(d.acceptedAnswers()).as("las respuestas aceptadas solo valen para SHORT").isEmpty();
        assertThat(ManageQuestionBank.validate(new QuestionInput(QuestionKind.SINGLE, "x", 10, "a".repeat(2000), TWO, null))
                .points()).isEqualTo(10);
    }

    @Test
    void trueFalseUsesFixedTextsAndNeedsExactlyOneCorrect() {
        QuizQuestion.Definition d = ManageQuestionBank.validate(q(QuestionKind.TRUE_FALSE,
                List.of(new OptionInput("lo que sea", false), new OptionInput("", true)), null));
        assertThat(d.options()).extracting(QuizOption::getText, QuizOption::isCorrect)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Verdadero", false),
                        org.assertj.core.groups.Tuple.tuple("Falso", true));
        String message = "Marca si la respuesta correcta es Verdadero o Falso.";
        rejects(q(QuestionKind.TRUE_FALSE, List.of(new OptionInput("V", true), new OptionInput("F", true)), null), message);
        rejects(q(QuestionKind.TRUE_FALSE, List.of(new OptionInput("V", false), new OptionInput("F", false)), null), message);
        rejects(q(QuestionKind.TRUE_FALSE, List.of(new OptionInput("V", true)), null), message);
        rejects(q(QuestionKind.TRUE_FALSE, null, null), message);
        rejects(q(QuestionKind.TRUE_FALSE, options(3), null), message);
    }

    @ParameterizedTest
    @EnumSource(value = QuestionKind.class, names = {"SINGLE", "MULTIPLE"})
    void choiceQuestionsNeedTwoToSixOptionsWithText(QuestionKind kind) {
        String count = "La pregunta necesita entre 2 y 6 opciones.";
        rejects(q(kind, null, null), count);
        rejects(q(kind, options(1), null), count);
        rejects(q(kind, options(7), null), count);
        assertThat(ManageQuestionBank.validate(q(kind, options(6), null)).options()).hasSize(6);
        String text = "Cada opción necesita un texto de hasta 500 caracteres.";
        rejects(q(kind, List.of(new OptionInput(" ", true), new OptionInput("B", false)), null), text);
        rejects(q(kind, List.of(new OptionInput("A", true), new OptionInput("b".repeat(501), false)), null), text);
        assertThat(ManageQuestionBank.validate(q(kind, List.of(new OptionInput("a".repeat(500), true),
                new OptionInput("B", false)), null)).options()).hasSize(2);
    }

    @Test
    void singleNeedsExactlyOneCorrectAndMultipleAtLeastOne() {
        List<OptionInput> none = List.of(new OptionInput("A", false), new OptionInput("B", false));
        List<OptionInput> both = List.of(new OptionInput("A", true), new OptionInput("B", true));
        rejects(q(QuestionKind.SINGLE, none, null), "Marca exactamente una opción correcta.");
        rejects(q(QuestionKind.SINGLE, both, null), "Marca exactamente una opción correcta.");
        rejects(q(QuestionKind.MULTIPLE, none, null), "Marca por lo menos una opción correcta.");
        assertThat(ManageQuestionBank.validate(q(QuestionKind.MULTIPLE, both, null)).options()).hasSize(2);
    }

    @Test
    void shortAnswersAreCleanedDedupedAndLimited() {
        List<String> given = new ArrayList<>(List.of(" 4 ", "cuatro", "4", "  "));
        given.add(null);
        QuizQuestion.Definition d = ManageQuestionBank.validate(q(QuestionKind.SHORT, TWO, given));
        assertThat(d.acceptedAnswers()).containsExactly("4", "cuatro");
        assertThat(d.options()).as("las opciones no cuentan en SHORT").isEmpty();
        String message = "Escribe entre 1 y 5 respuestas aceptadas de hasta 200 caracteres.";
        rejects(q(QuestionKind.SHORT, null, null), message);
        rejects(q(QuestionKind.SHORT, null, List.of(" ", "")), message);
        rejects(q(QuestionKind.SHORT, null, List.of("1", "2", "3", "4", "5", "6")), message);
        rejects(q(QuestionKind.SHORT, null, List.of("a".repeat(201))), message);
        assertThat(ManageQuestionBank.validate(q(QuestionKind.SHORT, null, List.of("1", "2", "3", "4", "5", "5")))
                .acceptedAnswers()).hasSize(5);
    }
}
