package co.edu.unipamplona.logicode.grading.domain;

/** Quién puso la nota. {@code AUTOMATIC}: calificación determinista de quizzes y casos de prueba (RF-16). */
public enum GradeSource {
    TEACHER,
    AI,
    AUTOMATIC
}
