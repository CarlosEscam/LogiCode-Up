package co.edu.unipamplona.logicode.activities.domain;

/**
 * RF-10. Los quizzes se responden en su propia página (módulo quizzes): {@code QUIZ} es calificable y
 * {@code PRACTICE_QUIZ} es de repaso, sin nota ni porcentaje (RF-21).
 */
public enum ActivityKind {
    TASK,
    WORKSHOP,
    EXAM,
    QUIZ,
    PRACTICE_QUIZ;

    public boolean isQuiz() {
        return this == QUIZ || this == PRACTICE_QUIZ;
    }
}
