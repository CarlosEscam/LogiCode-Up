package co.edu.unipamplona.logicode.system.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import co.edu.unipamplona.logicode.system.domain.SystemInfo;
import org.junit.jupiter.api.Test;

class GetSystemInfoTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Test
    void reportsBusinessTimeInConfiguredZoneAndSchemaVersion() {
        Clock clock = Clock.fixed(Instant.parse("2026-12-10T04:30:00Z"), BOGOTA);
        GetSystemInfo useCase = new GetSystemInfo(() -> Optional.of("1"), clock, new ApplicationVersion("0.1.0", "test"));

        SystemInfo info = useCase.execute();

        assertThat(info.application()).isEqualTo("LogiCode");
        assertThat(info.version()).isEqualTo("0.1.0");
        assertThat(info.environment()).isEqualTo("test");
        assertThat(info.schemaVersion()).isEqualTo("1");
        // 04:30 UTC del 10 de diciembre sigue siendo 9 de diciembre en Colombia (UTC-5).
        assertThat(info.businessTime().getDayOfMonth()).isEqualTo(9);
        assertThat(info.businessTime().getZone()).isEqualTo(BOGOTA);
    }

    @Test
    void schemaVersionIsNullWhenNoMigrationApplied() {
        GetSystemInfo useCase = new GetSystemInfo(Optional::empty, Clock.system(BOGOTA), new ApplicationVersion("dev", "local"));

        assertThat(useCase.execute().schemaVersion()).isNull();
    }
}
