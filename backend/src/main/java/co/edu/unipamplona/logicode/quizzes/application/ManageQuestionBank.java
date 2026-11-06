package co.edu.unipamplona.logicode.quizzes.application;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.activities.application.ActivityCatalog;
import co.edu.unipamplona.logicode.courses.application.CourseAccess;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.quizzes.domain.QuestionKind;
import co.edu.unipamplona.logicode.quizzes.domain.QuizOption;
import co.edu.unipamplona.logicode.quizzes.domain.QuizQuestion;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Banco de preguntas del curso (Etapa 7). Lo arma el docente del curso; los Administradores lo consultan. Una pregunta
 * que ya aparece en un intento no se edita, para que cada intento conserve la clave con que se calificó (RN-20).
 */
@Service
public class ManageQuestionBank {

    public record OptionInput(String text, boolean correct) {
    }

    public record QuestionInput(QuestionKind kind, String prompt, Integer points, String explanation,
            List<OptionInput> options, List<String> acceptedAnswers) {
    }

    static final int MAX_PROMPT = 2000;
    private static final int MAX_OPTION = 500;
    private static final int MAX_ANSWER = 200;

    private final QuestionRepository questions;
    private final QuizRepository quizzes;
    private final AttemptRepository attempts;
    private final CourseAccess courseAccess;
    private final ActivityCatalog catalog;
    private final Clock clock;

    public ManageQuestionBank(QuestionRepository questions, QuizRepository quizzes, AttemptRepository attempts,
            CourseAccess courseAccess, ActivityCatalog catalog, Clock clock) {
        this.questions = questions;
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.courseAccess = courseAccess;
        this.catalog = catalog;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<QuizViews.QuestionView> list(Actor actor, UUID courseId) {
        courseAccess.requireViewable(actor, courseId);
        if (!courseAccess.canManageContent(actor, courseId) && !courseAccess.canViewCourseWork(actor, courseId)) {
            throw ApplicationException.forbidden("Solo el docente del curso o un Administrador ve el banco de preguntas.");
        }
        return questions.findActiveByCourse(courseId).stream().map(q -> QuizViews.question(q, locked(q))).toList();
    }

    @Transactional
    public QuizViews.QuestionView create(Actor actor, UUID courseId, QuestionInput input) {
        courseAccess.requireManageContent(actor, courseId);
        QuizQuestion saved = questions.save(new QuizQuestion(courseId, validate(input), actor.userId(),
                clock.instant()));
        return QuizViews.question(saved, false);
    }

    @Transactional
    public QuizViews.QuestionView update(Actor actor, UUID questionId, QuestionInput input) {
        QuizQuestion question = requireManageable(actor, questionId);
        if (locked(question)) {
            throw ApplicationException.conflict("Esta pregunta ya se respondió en un intento y no se puede cambiar. "
                    + "Crea una copia y edítala.");
        }
        question.update(validate(input), clock.instant());
        return QuizViews.question(questions.save(question), false);
    }

    @Transactional
    public void archive(Actor actor, UUID questionId) {
        QuizQuestion question = requireManageable(actor, questionId);
        List<String> usedBy = quizzes.findUsing(questionId).stream()
                .map(quiz -> catalog.activity(quiz.getActivityId()).map(ActivityCatalog.ActivityInfo::title))
                .flatMap(Optional::stream).toList();
        if (!usedBy.isEmpty()) {
            throw ApplicationException.conflict("La pregunta está en " + String.join(", ", usedBy)
                    + ". Quítala primero de esos quizzes.");
        }
        question.archive(clock.instant());
        questions.save(question);
    }

    /** Usada en un quiz que ya tiene intentos. */
    boolean locked(QuizQuestion question) {
        return quizzes.findUsing(question.getId()).stream()
                .anyMatch(quiz -> attempts.existsByActivity(quiz.getActivityId()));
    }

    private QuizQuestion requireManageable(Actor actor, UUID questionId) {
        QuizQuestion question = questions.findById(questionId).filter(q -> !q.isArchived())
                .orElseThrow(() -> ApplicationException.notFound("La pregunta no existe."));
        courseAccess.requireManageContent(actor, question.getCourseId());
        return question;
    }

    // Visible para ManageQuestionBankValidationTest (pruebas de caracterización, guía 15).
    static QuizQuestion.Definition validate(QuestionInput input) {
        if (input.kind() == null) {
            throw ApplicationException.businessRule("Elige el tipo de pregunta.");
        }
        String prompt = clean(input.prompt());
        if (prompt == null || prompt.length() > MAX_PROMPT) {
            throw ApplicationException.businessRule("Escribe el enunciado de la pregunta (hasta " + MAX_PROMPT
                    + " caracteres).");
        }
        int points = input.points() == null ? 1 : input.points();
        if (points < 1 || points > QuizQuestion.MAX_POINTS) {
            throw ApplicationException.businessRule("Los puntos de la pregunta van de 1 a " + QuizQuestion.MAX_POINTS
                    + ".");
        }
        String explanation = clean(input.explanation());
        if (explanation != null && explanation.length() > MAX_PROMPT) {
            throw ApplicationException.businessRule("La explicación admite hasta " + MAX_PROMPT + " caracteres.");
        }
        List<OptionInput> given = input.options() == null ? List.of() : input.options();
        List<QuizOption> options = List.of();
        List<String> answers = List.of();
        switch (input.kind()) {
            case TRUE_FALSE -> {
                if (given.size() != 2 || given.get(0).correct() == given.get(1).correct()) {
                    throw ApplicationException.businessRule("Marca si la respuesta correcta es Verdadero o Falso.");
                }
                options = List.of(new QuizOption("Verdadero", given.get(0).correct()),
                        new QuizOption("Falso", given.get(1).correct()));
            }
            case SINGLE, MULTIPLE -> {
                if (given.size() < 2 || given.size() > QuizQuestion.MAX_OPTIONS) {
                    throw ApplicationException.businessRule("La pregunta necesita entre 2 y " + QuizQuestion.MAX_OPTIONS
                            + " opciones.");
                }
                options = given.stream().map(o -> {
                    String text = clean(o.text());
                    if (text == null || text.length() > MAX_OPTION) {
                        throw ApplicationException.businessRule("Cada opción necesita un texto de hasta " + MAX_OPTION
                                + " caracteres.");
                    }
                    return new QuizOption(text, o.correct());
                }).toList();
                long correct = options.stream().filter(QuizOption::isCorrect).count();
                if (input.kind() == QuestionKind.SINGLE && correct != 1) {
                    throw ApplicationException.businessRule("Marca exactamente una opción correcta.");
                }
                if (correct < 1) {
                    throw ApplicationException.businessRule("Marca por lo menos una opción correcta.");
                }
            }
            case SHORT -> {
                answers = (input.acceptedAnswers() == null ? List.<String>of() : input.acceptedAnswers()).stream()
                        .map(ManageQuestionBank::clean).filter(a -> a != null).distinct().toList();
                if (answers.isEmpty() || answers.size() > QuizQuestion.MAX_ACCEPTED_ANSWERS
                        || answers.stream().anyMatch(a -> a.length() > MAX_ANSWER)) {
                    throw ApplicationException.businessRule("Escribe entre 1 y " + QuizQuestion.MAX_ACCEPTED_ANSWERS
                            + " respuestas aceptadas de hasta " + MAX_ANSWER + " caracteres.");
                }
            }
        }
        return new QuizQuestion.Definition(input.kind(), prompt, points, explanation, options, answers);
    }

    static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.strip();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
