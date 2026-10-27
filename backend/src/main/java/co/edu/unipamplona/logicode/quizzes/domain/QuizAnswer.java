package co.edu.unipamplona.logicode.quizzes.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Respuesta a una pregunta: posiciones de las opciones elegidas o el texto escrito. */
@Embeddable
public class QuizAnswer {

    public static final int MAX_TEXT = 500;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    private String choices;

    @Column(name = "text_answer")
    private String textAnswer;

    private Boolean correct;

    protected QuizAnswer() {
    }

    public QuizAnswer(UUID questionId, Collection<Integer> choices, String textAnswer) {
        this.questionId = questionId;
        this.choices = choices == null || choices.isEmpty() ? null
                : new TreeSet<>(choices).stream().map(String::valueOf).collect(Collectors.joining(","));
        String text = textAnswer == null ? null : textAnswer.strip();
        if (text != null && text.length() > MAX_TEXT) {
            text = text.substring(0, MAX_TEXT);
        }
        this.textAnswer = text == null || text.isEmpty() ? null : text;
    }

    public Set<Integer> choiceSet() {
        if (choices == null) {
            return Set.of();
        }
        return Arrays.stream(choices.split(",")).map(Integer::valueOf).collect(Collectors.toCollection(TreeSet::new));
    }

    void grade(boolean isCorrect) {
        this.correct = isCorrect;
    }

    public boolean isBlank() {
        return choices == null && textAnswer == null;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public String getTextAnswer() {
        return textAnswer;
    }

    public Boolean getCorrect() {
        return correct;
    }
}
