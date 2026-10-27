package co.edu.unipamplona.logicode.administration.infrastructure;

import co.edu.unipamplona.logicode.administration.application.PlatformOverview;
import co.edu.unipamplona.logicode.administration.application.PlatformStatistics;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
class JdbcPlatformStatistics implements PlatformStatistics {

    private static final String SQL = """
            SELECT
              (SELECT count(*) FROM user_account WHERE role = 'STUDENT' AND status = 'ACTIVE'),
              (SELECT count(*) FROM user_account WHERE role = 'TEACHER' AND status = 'ACTIVE'),
              (SELECT count(*) FROM user_account WHERE role = 'ADMIN' AND status = 'ACTIVE'),
              (SELECT count(*) FROM user_account WHERE role = 'TEACHER' AND status = 'PENDING_APPROVAL'),
              (SELECT count(*) FROM user_account WHERE status = 'DISABLED'),
              (SELECT count(*) FROM course),
              (SELECT count(*) FROM activity WHERE status = 'PUBLISHED' AND archived_at IS NULL),
              (SELECT count(*) FROM submission),
              (SELECT count(*) FROM grade WHERE status = 'PENDING_REVIEW'),
              (SELECT count(*) FROM grade WHERE status = 'PROCESSING'),
              (SELECT count(*) FROM material WHERE visibility = 'PUBLIC' AND archived_at IS NULL),
              (SELECT count(*) FROM material WHERE visibility = 'PRIVATE' AND archived_at IS NULL),
              (SELECT coalesce(sum(size_bytes), 0) FROM stored_file),
              (SELECT count(*) FROM forum_thread WHERE deleted_at IS NULL),
              (SELECT count(*) FROM forum_post WHERE deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    JdbcPlatformStatistics(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PlatformOverview overview() {
        return jdbc.sql(SQL).query((rs, row) -> new PlatformOverview(rs.getLong(1), rs.getLong(2), rs.getLong(3),
                rs.getLong(4), rs.getLong(5), rs.getLong(6), rs.getLong(7), rs.getLong(8), rs.getLong(9),
                rs.getLong(10), rs.getLong(11), rs.getLong(12), rs.getLong(13), rs.getLong(14), rs.getLong(15)))
                .single();
    }
}
