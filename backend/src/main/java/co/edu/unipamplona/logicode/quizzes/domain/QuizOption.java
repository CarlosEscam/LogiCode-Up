package co.edu.unipamplona.logicode.quizzes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class QuizOption {

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private boolean correct;

    protected QuizOption() {
    }

    public QuizOption(String text, boolean correct) {
        this.text = text;
        this.correct = correct;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }
}
