package co.edu.unipamplona.logicode.administration.application;

/** Cifras del panel de administración. Cuentas inactivas y docentes por aprobar se cuentan aparte. */
public record PlatformOverview(long activeStudents, long activeTeachers, long activeAdmins, long pendingTeachers,
        long disabledAccounts, long courses, long publishedActivities, long submissions, long gradesInReview,
        long gradesProcessing, long publicMaterials, long privateMaterials, long storedFileBytes, long forumThreads,
        long forumReplies) {
}
