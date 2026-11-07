package co.edu.unipamplona.logicode.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class RequestIdFilterTest {

    @Test
    void theRequestLineDoesNotCarryTheCedula(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE",
                "/api/v1/courses/" + LogPathsTest.COURSE + "/enrollments/1000000205");
        new RequestIdFilter().doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(output).contains("DELETE /api/v1/courses/" + LogPathsTest.COURSE + "/enrollments/{cedula} 200")
                .doesNotContain("1000000205");
    }
}
