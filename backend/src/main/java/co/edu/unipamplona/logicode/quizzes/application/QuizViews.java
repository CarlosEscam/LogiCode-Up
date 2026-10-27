package co.edu.unipamplona.logicode.quizzes.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.edu.unipamplona.logicode.quizzes.domain.AttemptStatus;
import co.edu.unipamplona.logicode.quizzes.domain.QuestionKind;
import co.edu.unipamplona.logicode.quizzes.domain.QuizAnswer;
import co.edu.unipamplona.logicode.quizzes.domain.QuizAttempt;
import co.edu.unipamplona.logicode.quizzes.domain.QuizOption;
import co.edu.unipamplona.logicode.quizzes.domain.QuizQuestion;

/** Vistas del módulo. La clave de respuestas solo sale para el docente o en la revisión permitida. */
public final class QuizViews {

    /** Pregunta del banco con su clave, para quien administra o revisa el curso. */
    public record QuestionView(UUID id, QuestionKind kind, String prompt, int points, String explanation,
            List<OptionView> options, List<String> acceptedAnswers, boolean locked) {
    }

    public record OptionView(String text, boolean correct) {
    }

    /** {@code questions} solo para el docente y Administradores; el estudiante ve cuántas son y el tiempo. */
    public record QuizView(UUID activityId, int timeLimitMinutes, int questionCount, int maxPoints,
            List<QuestionView> questions, boolean hasAttempts) {
    }

    /**
     * Intento. {@code questions} existe al consultarlo uno por uno; {@code correct}, la clave y la explicación solo
     * cuando {@code reviewAvailable}.
     */
    public record AttemptView(UUID id, UUID activityId, boolean practice, int number, String studentName,
            AttemptStatus status, Instant startedAt, Instant deadline, Instant finishedAt, Integer correctCount,
            int questionCount, Integer points, int maxPoints, BigDecimal score, long durationSeconds,
            UUID submissionId, boolean reviewAvailable, List<AttemptQuestion> questions) {
    }

    public record AttemptQuestion(UUID id, QuestionKind kind, String prompt, int points, List<String> options,
            List<Integer> chosen, String text, Boolean correct, List<Integer> correctChoices,
            List<String> acceptedAnswers, String explanation) {
    }

    /** RN-14: por nota y luego por menor tiempo. {@code attemptId} solo para el docente. */
    public record RankingRow(int position, String studentName, BigDecimal score, int correctCount,
            int questionCount, long durationSeconds, boolean mine, UUID attemptId) {
    }

    private QuizViews() {
    }

    static QuestionView question(QuizQuestion q, boolean locked) {
        return new QuestionView(q.getId(), q.getKind(), q.getPrompt(), q.getPoints(), q.getExplanation(),
                q.getOptions().stream().map(o -> new OptionView(o.getText(), o.isCorrect())).toList(),
                q.getAcceptedAnswers(), locked);
    }

    static AttemptView attempt(QuizAttempt a, String studentName, boolean review, List<QuizQuestion> questions) {
        List<AttemptQuestion> items = null;
        if (questions != null) {
            Map<UUID, QuizAnswer> answers = a.getAnswers().stream()
                    .collect(Collectors.toMap(QuizAnswer::getQuestionId, Function.identity(), (x, y) -> y));
            items = questions.stream().map(q -> {
                QuizAnswer answer = answers.get(q.getId());
                return new AttemptQuestion(q.getId(), q.getKind(), q.getPrompt(), q.getPoints(),
                        q.getOptions().stream().map(QuizOption::getText).toList(),
                        answer == null ? List.of() : List.copyOf(answer.choiceSet()),
                        answer == null ? null : answer.getTextAnswer(),
                        review && answer != null ? answer.getCorrect() : null,
                        review ? List.copyOf(q.correctChoices()) : null,
                        review ? q.getAcceptedAnswers() : null,
                        review ? q.getExplanation() : null);
            }).toList();
        }
        return new AttemptView(a.getId(), a.getActivityId(), a.isPractice(), a.getNumber(), studentName,
                a.getStatus(), a.getStartedAt(), a.getDeadline(), a.getFinishedAt(), a.getCorrectCount(),
                a.getQuestionCount(), a.getPoints(), a.getMaxPoints(), a.getScore(), a.durationSeconds(),
                a.getSubmissionId(), review, items);
    }
}
