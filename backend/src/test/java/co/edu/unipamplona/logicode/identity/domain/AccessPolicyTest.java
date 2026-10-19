package co.edu.unipamplona.logicode.identity.domain;

import static co.edu.unipamplona.logicode.identity.domain.Permission.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccessPolicyTest {

    @Test
    void ordinaryAdminCannotAnswerCreateGradeOrChangeRoles() {
        var permissions = AccessPolicy.permissionsFor(Role.ADMIN, AccountStatus.ACTIVE, false);

        assertThat(permissions).doesNotContain(ANSWER_ACTIVITIES, MANAGE_COURSE_CONTENT, GRADE, CHANGE_ROLES);
        assertThat(permissions).contains(APPROVE_TEACHERS, MANAGE_USERS, MODERATE_FORUM, VIEW_COURSE_GRADES,
                MANAGE_BACKUPS);
    }

    @Test
    void fullPrivilegeAdminHasEveryPermission() {
        assertThat(AccessPolicy.permissionsFor(Role.ADMIN, AccountStatus.ACTIVE, true))
                .containsExactlyInAnyOrder(Permission.values());
    }

    @Test
    void fullPrivilegeOnlyAppliesToAdminRole() {
        assertThat(AccessPolicy.permissionsFor(Role.TEACHER, AccountStatus.ACTIVE, true)).doesNotContain(CHANGE_ROLES);
    }

    @Test
    void pendingOrDisabledAccountsHaveNoPermissions() {
        assertThat(AccessPolicy.permissionsFor(Role.TEACHER, AccountStatus.PENDING_APPROVAL, false)).isEmpty();
        assertThat(AccessPolicy.permissionsFor(Role.ADMIN, AccountStatus.DISABLED, true)).isEmpty();
    }

    @Test
    void studentAndTeacherPermissions() {
        assertThat(AccessPolicy.permissionsFor(Role.STUDENT, AccountStatus.ACTIVE, false))
                .containsExactlyInAnyOrder(VIEW_PRIVATE_COURSE_CONTENT, PARTICIPATE_FORUM, ANSWER_ACTIVITIES);
        assertThat(AccessPolicy.permissionsFor(Role.TEACHER, AccountStatus.ACTIVE, false))
                .contains(MANAGE_COURSE_CONTENT, GRADE, MANAGE_STUDENT_ENABLEMENTS)
                .doesNotContain(CHANGE_ROLES, ANSWER_ACTIVITIES, APPROVE_TEACHERS);
    }
}
