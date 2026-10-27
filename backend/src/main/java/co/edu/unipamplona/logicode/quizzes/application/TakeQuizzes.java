package co.edu.unipamplona.logicode.quizzes.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import co.edu.unipamplona.logicode.activities.application.ActivityCatalog;
import co.edu.unipamplona.logicode.activities.application.SubmitWork;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.UserDirectory;
import co.edu.unipamplona.logicode.quizzes.domain.QuestionKind;
import co.edu.unipamplona.logicode.quizzes.domain.Quiz;
import co.edu.unipamplona.logicode.quizzes.domain.QuizAnswer;
import co.edu.unipamplona.logicode.quizzes.domain.QuizAttempt;
import co.edu.unipamplona.logicode.quizzes.domain.QuizQuestion;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import co.edu.unipamplona.logicode.shared.domain.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * El estudiante inscrito responde quizzes (RF-10, RF-21, RN-13, RN-14): un intento a la vez, con el tiempo total
 * corriendo desde que empieza; las respuestas se guardan mientras responde y, al agotarse el tiempo, el intento se
 * envía solo. El quiz calificable registra cada intento como entrega con su nota; el de repaso no tiene nota y muestra
 * la revisión al terminar. La revisión de un calificable se abre cuando el quiz cierra.
 */
@Service
public class TakeQuizzes {

    public record AnswerInput(UUID questionId, List<Integer> choices, String text) {
    }

    private static final Logger log = LoggerFactory.getLogger(TakeQuizzes.class);

    private final AttemptRepository attempts;
    private final QuizRepository quizzes;
    private final ManageQuizzes manageQuizzes;
    private final ActivityCatalog catalog;
    private final SubmitWork submitWork;
    private final UserDirectory directory;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public TakeQuizzes(AttemptRepository attempts, QuizRepository quizzes, ManageQuizzes manageQuizzes,
            ActivityCatalog catalog, SubmitWork submitWork, UserDirectory directory,
            PlatformTransactionManager transactionManager, Clock clock) {
        this.attempts = attempts;
        this.quizzes = quizzes;
        this.manageQuizzes = manageQuizzes;
        this.catalog = catalog;
        this.submitWork = submitWork;
        this.directory = directory;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /** Empieza un intento, o devuelve el que está en curso. */
    @Transactional
    public QuizViews.AttemptView start(Actor actor, UUID activityId) {
        actor.require(Permission.ANSWER_ACTIVITIES);
        ActivityCatalog.QuizActivity activity = catalog.requireQuiz(actor, activityId);
        if (activity.progress() == null) {
            throw ApplicationException.forbidden("Solo los estudiantes inscritos en el curso responden el quiz.");
        }
        Instant now = clock.instant();
        List<QuizAttempt> mine = settle(attempts.findByActivityAndStudent(activityId, actor.userId()), now);
        Optional<QuizAttempt> open = mine.stream().filter(a -> !a.isFinished()).findFirst();
        Quiz quiz = quizzes.findById(activityId)
                .orElseThrow(() -> ApplicationException.businessRule("El quiz aún no tiene preguntas."));
        List<QuizQuestion> questions = manageQuizzes.ordered(quiz);
        if (open.isPresent()) {
            return QuizViews.attempt(open.get(), null, false, questions);
        }
        activity = catalog.requireQuiz(actor, activityId);
        Instant closes = activity.practice() ? activity.closesAt() : activity.progress().deadline();
        if (now.isBefore(activity.opensAt())) {
            throw ApplicationException.businessRule("El quiz aún no está abierto.");
        }
        if (now.isAfter(closes)) {
            throw ApplicationException.businessRule("El quiz ya cerró. Si necesitas más plazo, habla con tu docente.");
        }
        if (!activity.practice() && activity.progress().attemptsUsed() >= activity.progress().attemptsAllowed()) {
            throw ApplicationException.businessRule("Ya usaste los intentos permitidos para este quiz.");
        }
        Instant byTime = now.plus(Duration.ofMinutes(quiz.getTimeLimitMinutes()));
        QuizAttempt attempt = attempts.save(new QuizAttempt(activityId, actor.userId(), mine.size() + 1,
                activity.practice(), questions, now, byTime.isBefore(closes) ? byTime : closes));
        return QuizViews.attempt(attempt, null, false, questions);
    }

    /** Guarda las respuestas mientras el estudiante responde. */
    @Transactional
    public void saveAnswers(Actor actor, UUID attemptId, List<AnswerInput> answers) {
        QuizAttempt attempt = requireOwn(actor, attemptId);
        Instant now = clock.instant();
        if (attempt.isFinished()) {
            throw ApplicationException.conflict("El intento ya se envió.");
        }
        List<QuizQuestion> questions = questionsOf(attempt);
        if (attempt.isExpiredAt(now)) {
            complete(attempt, questions, now);
            throw ApplicationException.conflict("Se acabó el tiempo: el intento se envió con las respuestas guardadas.");
        }
        attempt.saveAnswers(validate(answers, questions), now);
        attempts.save(attempt);
    }

    /** Envía el intento. Después del tiempo (y su margen) se envía con lo último que se guardó. */
    @Transactional
    public QuizViews.AttemptView finish(Actor actor, UUID attemptId, List<AnswerInput> answers) {
        QuizAttempt attempt = requireOwn(actor, attemptId);
        List<QuizQuestion> questions = questionsOf(attempt);
        Instant now = clock.instant();
        if (!attempt.isFinished()) {
            if (answers != null && !attempt.isExpiredAt(now)) {
                attempt.saveAnswers(validate(answers, questions), now);
            }
            complete(attempt, questions, now);
        }
        return view(actor, attempt, questions, now);
    }

    @Transactional
    public QuizViews.AttemptView get(Actor actor, UUID attemptId) {
        QuizAttempt attempt = attempts.findById(attemptId)
                .orElseThrow(() -> ApplicationException.notFound("El intento no existe."));
        ActivityCatalog.QuizActivity activity = catalog.requireQuiz(actor, attempt.getActivityId());
        if (!attempt.getStudentId().equals(actor.userId()) && !activity.canViewWork()) {
            throw ApplicationException.notFound("El intento no existe.");
        }
        List<QuizQuestion> questions = questionsOf(attempt);
        Instant now = clock.instant();
        if (attempt.isExpiredAt(now)) {
            complete(attempt, questions, now);
        }
        return view(actor, attempt, questions, now);
    }

    /** Mis intentos en el quiz, del primero al último, sin las preguntas. */
    @Transactional
    public List<QuizViews.AttemptView> mine(Actor actor, UUID activityId) {
        ActivityCatalog.QuizActivity activity = catalog.requireQuiz(actor, activityId);
        Instant now = clock.instant();
        boolean closed = now.isAfter(activity.closesAt());
        return settle(attempts.findByActivityAndStudent(activityId, actor.userId()), now).stream()
                .map(a -> QuizViews.attempt(a, null, a.isFinished() && (a.isPractice() || closed), null)).toList();
    }

    /** RN-14: un renglón por estudiante con intentos terminados. Lo ven los inscritos y quien ve el curso. */
    @Transactional(readOnly = true)
    public List<QuizViews.RankingRow> ranking(Actor actor, UUID activityId) {
        ActivityCatalog.QuizActivity activity = catalog.requireQuiz(actor, activityId);
        if (activity.progress() == null && !activity.canViewWork()) {
            throw ApplicationException.forbidden("El ranking lo ven los estudiantes del curso y su docente.");
        }
        Comparator<QuizAttempt> best = Comparator.comparing(QuizAttempt::getScore).reversed()
                .thenComparingLong(QuizAttempt::durationSeconds);
        // El calificable cuenta el último intento, como su nota (T-33); el de repaso, el mejor.
        Collection<QuizAttempt> counted = attempts.findFinishedByActivity(activityId).stream()
                .collect(Collectors.toMap(QuizAttempt::getStudentId, Function.identity(), (a, b) -> activity.practice()
                        ? (best.compare(a, b) <= 0 ? a : b) : (a.getNumber() >= b.getNumber() ? a : b)))
                .values();
        List<QuizAttempt> sorted = counted.stream().sorted(best).toList();
        Map<UUID, String> names = directory.fullNamesOf(sorted.stream().map(QuizAttempt::getStudentId).toList());
        return IntStream.range(0, sorted.size()).mapToObj(i -> {
            QuizAttempt a = sorted.get(i);
            return new QuizViews.RankingRow(i + 1, names.get(a.getStudentId()), a.getScore(), a.getCorrectCount(),
                    a.getQuestionCount(), a.durationSeconds(), a.getStudentId().equals(actor.userId()),
                    activity.canViewWork() ? a.getId() : null);
        }).toList();
    }

    /** RN-13: envía los intentos cuyo tiempo se agotó sin que el navegador los enviara. Cada uno en su transacción. */
    public int finishExpired() {
        Instant cutoff = clock.instant().minus(QuizAttempt.GRACE);
        int finished = 0;
        for (QuizAttempt candidate : attempts.findOpenWithDeadlineBefore(cutoff)) {
            try {
                Boolean done = transaction.execute(status -> attempts.findById(candidate.getId())
                        .filter(a -> a.isExpiredAt(clock.instant())).map(a -> {
                            complete(a, questionsOf(a), clock.instant());
                            return true;
                        }).orElse(false));
                if (Boolean.TRUE.equals(done)) {
                    finished++;
                }
            } catch (RuntimeException e) {
                log.warn("No fue posible enviar el intento vencido {}", candidate.getId(), e);
            }
        }
        return finished;
    }

    private List<QuizAttempt> settle(List<QuizAttempt> list, Instant now) {
        for (QuizAttempt attempt : list) {
            if (attempt.isExpiredAt(now)) {
                complete(attempt, questionsOf(attempt), now);
            }
        }
        return list;
    }

    private void complete(QuizAttempt attempt, List<QuizQuestion> questions, Instant now) {
        attempt.finish(questions, now);
        if (!attempt.isPractice()) {
            attempt.linkSubmission(submitWork.recordQuizAttempt(attempt.getActivityId(), attempt.getStudentId(),
                    summary(attempt, questions), attempt.getFinishedAt(), attempt.getScore(), feedback(attempt)));
        }
        attempts.save(attempt);
    }

    private QuizViews.AttemptView view(Actor actor, QuizAttempt attempt, List<QuizQuestion> questions, Instant now) {
        ActivityCatalog.QuizActivity activity = catalog.requireQuiz(actor, attempt.getActivityId());
        boolean review = attempt.isFinished()
                && (attempt.isPractice() || activity.canViewWork() || now.isAfter(activity.closesAt()));
        String name = directory.fullNamesOf(List.of(attempt.getStudentId())).get(attempt.getStudentId());
        return QuizViews.attempt(attempt, name, review, questions);
    }

    private QuizAttempt requireOwn(Actor actor, UUID attemptId) {
        return attempts.findById(attemptId).filter(a -> a.getStudentId().equals(actor.userId()))
                .orElseThrow(() -> ApplicationException.notFound("El intento no existe."));
    }

    private List<QuizQuestion> questionsOf(QuizAttempt attempt) {
        return manageQuizzes.ordered(quizzes.findById(attempt.getActivityId()).orElseThrow());
    }

    private static List<QuizAnswer> validate(List<AnswerInput> answers, List<QuizQuestion> questions) {
        Map<UUID, QuizQuestion> byId = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, Function.identity()));
        List<QuizAnswer> valid = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (AnswerInput answer : answers) {
            QuizQuestion question = byId.get(answer.questionId());
            if (question == null || !seen.add(answer.questionId())) {
                throw ApplicationException.businessRule("Una respuesta no corresponde a las preguntas del quiz.");
            }
            List<Integer> choices = answer.choices() == null ? List.of() : answer.choices();
            boolean single = question.getKind() == QuestionKind.SINGLE || question.getKind() == QuestionKind.TRUE_FALSE;
            if (choices.stream().anyMatch(c -> c == null || c < 0 || c >= question.getOptions().size())
                    || single && choices.size() > 1 || question.getKind() == QuestionKind.SHORT && !choices.isEmpty()) {
                throw ApplicationException.businessRule("Una respuesta tiene opciones que no existen.");
            }
            valid.add(new QuizAnswer(question.getId(), choices,
                    question.getKind() == QuestionKind.SHORT ? answer.text() : null));
        }
        return valid;
    }

    /** Texto de la entrega: lo que respondió el estudiante, sin decir si acertó. */
    private static String summary(QuizAttempt attempt, List<QuizQuestion> questions) {
        Map<UUID, QuizAnswer> answers = attempt.getAnswers().stream()
                .collect(Collectors.toMap(QuizAnswer::getQuestionId, Function.identity(), (a, b) -> b));
        StringBuilder text = new StringBuilder("Respuestas del quiz (intento " + attempt.getNumber() + "):");
        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            QuizAnswer answer = answers.get(q.getId());
            String given;
            if (answer == null || answer.isBlank()) {
                given = "sin responder";
            } else if (q.getKind() == QuestionKind.SHORT) {
                given = "«" + answer.getTextAnswer() + "»";
            } else {
                given = answer.choiceSet().stream().map(c -> "«" + q.getOptions().get(c).getText() + "»")
                        .collect(Collectors.joining(", "));
            }
            text.append("\n").append(i + 1).append(". ").append(given);
        }
        return text.toString();
    }

    private static String feedback(QuizAttempt attempt) {
        return "Respuestas correctas: " + attempt.getCorrectCount() + " de " + attempt.getQuestionCount() + " ("
                + attempt.getPoints() + " de " + attempt.getMaxPoints() + " puntos).";
    }
}
