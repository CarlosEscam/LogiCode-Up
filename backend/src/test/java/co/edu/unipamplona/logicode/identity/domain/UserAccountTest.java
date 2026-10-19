package co.edu.unipamplona.logicode.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class UserAccountTest {

    private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");

    @Test
    void changingPasswordKeepsOnlyCurrentAndImmediatelyPreviousHash() {
        UserAccount account = UserAccount.registerStudent("1001", "Ana Pérez", "ana@example.com", "hash-1", NOW);

        account.changePassword("hash-2", NOW);
        account.changePassword("hash-3", NOW);

        assertThat(account.getPasswordHash()).isEqualTo("hash-3");
        assertThat(account.getPreviousPasswordHash()).isEqualTo("hash-2");
    }

    @Test
    void teacherRequestStartsPendingAndCanBeApprovedOnce() {
        UserAccount teacher = UserAccount.requestTeacher("2002", "Luis Díaz", "luis@example.com", "h", NOW);
        assertThat(teacher.permissions()).isEmpty();

        teacher.approveAsTeacher(NOW);

        assertThat(teacher.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(teacher.hasPermission(Permission.GRADE)).isTrue();
        assertThatThrownBy(() -> teacher.approveAsTeacher(NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void leavingAdminRoleDropsFullPrivileges() {
        UserAccount admin = UserAccount.initialAdmin("3003", "Admin", "admin@example.com", NOW);
        assertThat(admin.isActiveFullAdmin()).isTrue();
        assertThat(admin.hasPassword()).isFalse();

        admin.changeRole(Role.TEACHER, NOW);

        assertThat(admin.isFullPrivileges()).isFalse();
    }

    @Test
    void tokenIsSingleUseAndExpires() {
        OneTimeToken token = OneTimeToken.passwordReset(java.util.UUID.randomUUID(), "h", NOW);

        assertThat(token.isUsable(NOW.plusSeconds(59 * 60))).isTrue();
        assertThat(token.isUsable(NOW.plusSeconds(60 * 60))).isFalse();
        token.consume(NOW);
        assertThat(token.isUsable(NOW)).isFalse();
    }
}
