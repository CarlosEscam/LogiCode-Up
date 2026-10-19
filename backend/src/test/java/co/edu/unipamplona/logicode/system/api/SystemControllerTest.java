package co.edu.unipamplona.logicode.system.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import co.edu.unipamplona.logicode.shared.infrastructure.web.RequestIdFilter;
import co.edu.unipamplona.logicode.system.application.GetSystemInfo;
import co.edu.unipamplona.logicode.system.domain.SystemInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SystemController.class)
@Import(RequestIdFilter.class)
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetSystemInfo getSystemInfo;

    @Test
    void returnsSystemInfoUnderVersionedPrefix() throws Exception {
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 18, 20, 0, 0, 0, ZoneId.of("America/Bogota"));
        when(getSystemInfo.execute()).thenReturn(new SystemInfo("LogiCode", "0.1.0", "test", now, "1"));

        mockMvc.perform(get("/api/v1/system/info"))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestIdFilter.HEADER))
                .andExpect(jsonPath("$.application").value("LogiCode"))
                .andExpect(jsonPath("$.environment").value("test"))
                .andExpect(jsonPath("$.timeZone").value("America/Bogota"))
                .andExpect(jsonPath("$.schemaVersion").value("1"));
    }

    @Test
    void unknownRouteReturnsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void keepsValidIncomingRequestId() throws Exception {
        when(getSystemInfo.execute()).thenReturn(new SystemInfo("LogiCode", "0.1.0", "test",
                ZonedDateTime.now(ZoneId.of("America/Bogota")), null));

        mockMvc.perform(get("/api/v1/system/info").header(RequestIdFilter.HEADER, "abc12345-trace"))
                .andExpect(header().string(RequestIdFilter.HEADER, "abc12345-trace"));
    }
}
