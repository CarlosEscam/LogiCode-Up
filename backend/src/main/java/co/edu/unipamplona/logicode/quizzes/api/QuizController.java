package co.edu.unipamplona.logicode.quizzes.api;

import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.CurrentActor;
import co.edu.unipamplona.logicode.quizzes.application.ManageQuestionBank;
import co.edu.unipamplona.logicode.quizzes.application.ManageQuizzes;
import co.edu.unipamplona.logicode.quizzes.application.QuizViews;
import co.edu.unipamplona.logicode.quizzes.application.TakeQuizzes;
import co.edu.unipamplona.logicode.quizzes.domain.QuestionKind;
import co.edu.unipamplona.logicode.shared.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1)
@Tag(name = "Quizzes", description = "Banco de preguntas, quizzes calificables y de repaso, intentos y ranking (RF-10, RF-21, RN-13, RN-14)")
public class QuizController {

    public record OptionRequest(@Size(max = 500) String text, boolean correct) {
    }

    public record QuestionRequest(@NotNull QuestionKind kind, @NotNull @Size(max = 2000) String prompt,
            @Min(1) @Max(10) Integer points, @Size(max = 2000) String explanation,
            @Size(max = 6) List<@Valid OptionRequest> options, @Size(max = 5) List<@Size(max = 200) String> acceptedAnswers) {

        ManageQuestionBank.QuestionInput input() {
            return new ManageQuestionBank.QuestionInput(kind, prompt, points, explanation, options == null ? List.of()
                    : options.stream().map(o -> new ManageQuestionBank.OptionInput(o.text(), o.correct())).toList(),
                    acceptedAnswers);
        }
    }

    public record QuizRequest(@Min(1) @Max(300) int timeLimitMinutes, @NotNull @Size(max = 50) List<UUID> questionIds) {
    }

    public record AnswerRequest(@NotNull UUID questionId, @Size(max = 6) List<Integer> choices,
            @Size(max = 500) String text) {
    }

    public record AnswersRequest(@NotNull @Size(max = 50) List<@Valid AnswerRequest> answers) {

        List<TakeQuizzes.AnswerInput> inputs() {
            return answers.stream().map(a -> new TakeQuizzes.AnswerInput(a.questionId(), a.choices(), a.text()))
                    .toList();
        }
    }

    private final ManageQuestionBank bank;
    private final ManageQuizzes manageQuizzes;
    private final TakeQuizzes takeQuizzes;

    public QuizController(ManageQuestionBank bank, ManageQuizzes manageQuizzes, TakeQuizzes takeQuizzes) {
        this.bank = bank;
        this.manageQuizzes = manageQuizzes;
        this.takeQuizzes = takeQuizzes;
    }

    @GetMapping("/courses/{courseId}/questions")
    @Operation(summary = "Banco de preguntas del curso con su clave (docente del curso y Administradores)")
    public List<QuizViews.QuestionView> questions(@CurrentActor Actor actor, @PathVariable UUID courseId) {
        return bank.list(actor, courseId);
    }

    @PostMapping("/courses/{courseId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar una pregunta al banco del curso")
    public QuizViews.QuestionView createQuestion(@CurrentActor Actor actor, @PathVariable UUID courseId,
            @Valid @RequestBody QuestionRequest request) {
        return bank.create(actor, courseId, request.input());
    }

    @PutMapping("/questions/{questionId}")
    @Operation(summary = "Editar una pregunta (409 si ya se respondió en un intento)")
    public QuizViews.QuestionView updateQuestion(@CurrentActor Actor actor, @PathVariable UUID questionId,
            @Valid @RequestBody QuestionRequest request) {
        return bank.update(actor, questionId, request.input());
    }

    @DeleteMapping("/questions/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archivar una pregunta que ningún quiz usa")
    public void archiveQuestion(@CurrentActor Actor actor, @PathVariable UUID questionId) {
        bank.archive(actor, questionId);
    }

    @GetMapping("/activities/{activityId}/quiz")
    @Operation(summary = "Tiempo y preguntas del quiz (204 si aún no tiene); el estudiante no ve las preguntas aquí")
    public ResponseEntity<QuizViews.QuizView> quiz(@CurrentActor Actor actor, @PathVariable UUID activityId) {
        return manageQuizzes.get(actor, activityId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/activities/{activityId}/quiz")
    @Operation(summary = "Armar el quiz: tiempo total y preguntas del banco, en orden (guarda su rúbrica)")
    public QuizViews.QuizView defineQuiz(@CurrentActor Actor actor, @PathVariable UUID activityId,
            @Valid @RequestBody QuizRequest request) {
        return manageQuizzes.define(actor, activityId, request.timeLimitMinutes(), request.questionIds());
    }

    @PostMapping("/activities/{activityId}/quiz/attempts")
    @Operation(summary = "Empezar un intento, o continuar el que está en curso")
    public QuizViews.AttemptView start(@CurrentActor Actor actor, @PathVariable UUID activityId) {
        return takeQuizzes.start(actor, activityId);
    }

    @GetMapping("/activities/{activityId}/quiz/attempts/mine")
    @Operation(summary = "Mis intentos en el quiz")
    public List<QuizViews.AttemptView> mine(@CurrentActor Actor actor, @PathVariable UUID activityId) {
        return takeQuizzes.mine(actor, activityId);
    }

    @GetMapping("/activities/{activityId}/quiz/ranking")
    @Operation(summary = "Ranking por nota y menor tiempo (RN-14)")
    public List<QuizViews.RankingRow> ranking(@CurrentActor Actor actor, @PathVariable UUID activityId) {
        return takeQuizzes.ranking(actor, activityId);
    }

    @GetMapping("/quiz-attempts/{attemptId}")
    @Operation(summary = "Un intento con sus preguntas; la revisión, cuando está disponible")
    public QuizViews.AttemptView attempt(@CurrentActor Actor actor, @PathVariable UUID attemptId) {
        return takeQuizzes.get(actor, attemptId);
    }

    @PutMapping("/quiz-attempts/{attemptId}/answers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Guardar las respuestas mientras se responde")
    public void saveAnswers(@CurrentActor Actor actor, @PathVariable UUID attemptId,
            @Valid @RequestBody AnswersRequest request) {
        takeQuizzes.saveAnswers(actor, attemptId, request.inputs());
    }

    @PostMapping("/quiz-attempts/{attemptId}/finish")
    @Operation(summary = "Enviar el intento y ver su resultado")
    public QuizViews.AttemptView finish(@CurrentActor Actor actor, @PathVariable UUID attemptId,
            @Valid @RequestBody(required = false) AnswersRequest request) {
        return takeQuizzes.finish(actor, attemptId, request == null ? null : request.inputs());
    }
}
