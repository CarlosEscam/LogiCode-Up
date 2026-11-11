package co.edu.unipamplona.logicode.quizzes.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import co.edu.unipamplona.logicode.activities.application.ActivityCatalog;
import co.edu.unipamplona.logicode.grading.application.ManageRubrics;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.quizzes.domain.QuestionKind;
import co.edu.unipamplona.logicode.quizzes.domain.Quiz;
import co.edu.unipamplona.logicode.quizzes.domain.QuizOption;
import co.edu.unipamplona.logicode.quizzes.domain.QuizQuestion;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El docente arma el quiz con preguntas de su banco y el tiempo total (RN-13). Al guardarlo, las preguntas y su clave
 * quedan como la rúbrica versionada de la actividad (RN-20). Con el primer intento, el quiz ya no cambia.
 */
@Service
public class ManageQuizzes {

    private final QuizRepository quizzes;
    private final QuestionRepository questions;
    private final AttemptRepository attempts;
    private final ActivityCatalog catalog;
    private final ManageRubrics rubrics;
    private final ManageQuestionBank bank;
    private final Clock clock;

    public ManageQuizzes(QuizRepository quizzes, QuestionRepository questions, AttemptRepository attempts,
            ActivityCatalog catalog, ManageRubrics rubrics, ManageQuestionBank bank, Clock clock) {
        this.quizzes = quizzes;
        this.questions = questions;
        this.attempts = attempts;
        this.catalog = catalog;
        this.rubrics = rubrics;
        this.bank = bank;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<QuizViews.QuizView> get(Actor actor, UUID activityId) {
        ActivityCatalog.QuizActivity activity = catalog.requireQuiz(actor, activityId);
        boolean staff = activity.canManage() || activity.canViewWork();
        return quizzes.findById(activityId).map(quiz -> {
            List<QuizQuestion> items = ordered(quiz);
            return new QuizViews.QuizView(activityId, quiz.getTimeLimitMinutes(), items.size(),
                    items.stream().mapToInt(QuizQuestion::getPoints).sum(),
                    staff ? items.stream().map(q -> QuizViews.question(q, bank.locked(q))).toList() : null,
                    attempts.existsByActivity(activityId));
        });
    }

    @Transactional
    public QuizViews.QuizView define(Actor actor, UUID activityId, int timeLimitMinutes, List<UUID> questionIds) {
        ActivityCatalog.QuizActivity activity = catalog.requireQuiz(actor, activityId);
        if (!activity.canManage()) {
            throw ApplicationException.forbidden("Solo el docente del curso arma el quiz.");
        }
        if (attempts.existsByActivity(activityId)) {
            throw ApplicationException.conflict("El quiz ya tiene intentos: sus preguntas y su tiempo no cambian.");
        }
        if (timeLimitMinutes < 1 || timeLimitMinutes > Quiz.MAX_MINUTES) {
            throw ApplicationException.businessRule("El tiempo total va de 1 a " + Quiz.MAX_MINUTES + " minutos.");
        }
        List<UUID> ids = questionIds == null ? List.of() : questionIds;
        if (ids.isEmpty() || ids.size() > Quiz.MAX_QUESTIONS || ids.stream().distinct().count() != ids.size()) {
            throw ApplicationException.businessRule("Elige entre 1 y " + Quiz.MAX_QUESTIONS
                    + " preguntas distintas del banco.");
        }
        Map<UUID, QuizQuestion> found = questions.findAllById(ids).stream()
                .collect(Collectors.toMap(QuizQuestion::getId, Function.identity()));
        List<QuizQuestion> chosen = new ArrayList<>();
        for (UUID id : ids) {
            QuizQuestion question = found.get(id);
            if (question == null || question.isArchived() || !question.getCourseId().equals(activity.courseId())) {
                throw ApplicationException.businessRule("Una de las preguntas no está en el banco de este curso.");
            }
            requireNotSharedAcrossKinds(question, activity);
            chosen.add(question);
        }
        Quiz quiz = quizzes.findById(activityId).orElse(null);
        if (quiz == null) {
            quiz = new Quiz(activityId, timeLimitMinutes, ids, clock.instant());
        } else {
            quiz.define(timeLimitMinutes, ids, clock.instant());
        }
        quizzes.save(quiz);
        rubrics.defineForQuiz(activityId, answerKey(chosen), IntStream.range(0, chosen.size()).mapToObj(i ->
                new ManageRubrics.CriterionInput("Pregunta " + (i + 1), chosen.get(i).getPrompt(),
                        BigDecimal.valueOf(chosen.get(i).getPoints()))).toList(), actor.userId());
        return new QuizViews.QuizView(activityId, timeLimitMinutes, chosen.size(),
                chosen.stream().mapToInt(QuizQuestion::getPoints).sum(),
                chosen.stream().map(q -> QuizViews.question(q, false)).toList(), false);
    }

    /**
     * P-04 (pentest 2026-11-11): al terminar un repaso el estudiante ve la respuesta de cada pregunta, así que una
     * pregunta no puede estar a la vez en un repaso y en un quiz calificable.
     */
    private void requireNotSharedAcrossKinds(QuizQuestion question, ActivityCatalog.QuizActivity activity) {
        for (Quiz other : quizzes.findUsing(question.getId())) {
            boolean clash = !other.getActivityId().equals(activity.id()) && catalog.activity(other.getActivityId())
                    .filter(o -> o.practice() != activity.practice()).isPresent();
            if (clash) {
                throw ApplicationException.businessRule("La pregunta «" + question.getPrompt() + "» ya está en un "
                        + (activity.practice() ? "quiz calificable" : "quiz de repaso") + ". El repaso muestra las "
                        + "respuestas al terminar: usa preguntas distintas en los repasos y en los quizzes calificables.");
            }
        }
    }

    /** Preguntas del quiz en su orden. */
    List<QuizQuestion> ordered(Quiz quiz) {
        Map<UUID, QuizQuestion> byId = questions.findAllById(quiz.getQuestionIds()).stream()
                .collect(Collectors.toMap(QuizQuestion::getId, Function.identity()));
        return quiz.getQuestionIds().stream().map(byId::get).toList();
    }

    /** Respuesta esperada de la rúbrica: la clave de cada pregunta. */
    private static String answerKey(List<QuizQuestion> chosen) {
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < chosen.size(); i++) {
            QuizQuestion q = chosen.get(i);
            String answer = q.getKind() == QuestionKind.SHORT ? String.join(" / ", q.getAcceptedAnswers())
                    : q.getOptions().stream().filter(QuizOption::isCorrect).map(QuizOption::getText)
                            .collect(Collectors.joining(" + "));
            key.append(i + 1).append(". ").append(answer).append('\n');
        }
        return key.toString().strip();
    }
}
