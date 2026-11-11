package co.edu.unipamplona.logicode.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import co.edu.unipamplona.logicode.identity.application.TrustedDevices;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AbuseLimitFilterTest {

    Instant now = Instant.parse("2026-11-11T05:00:00Z");
    final Clock clock = new Clock() {
        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    };

    @Test
    void teacherRequestsFromOneAddressStopAtTheLimit() throws Exception {
        AbuseLimitFilter filter = new AbuseLimitFilter(clock, new TrustedDevices(clock));
        for (int i = 0; i < 10; i++) {
            assertThat(send(filter, "POST", "/api/v1/auth/teacher-requests", "10.0.0.1", 201)).isEqualTo(201);
        }
        assertThat(send(filter, "POST", "/api/v1/auth/teacher-requests", "10.0.0.1", 201)).isEqualTo(429);
        assertThat(send(filter, "POST", "/api/v1/auth/teacher-requests", "10.0.0.2", 201)).as("otra IP").isEqualTo(201);
        assertThat(send(filter, "GET", "/api/v1/auth/teacher-requests", "10.0.0.1", 200)).as("otro método").isEqualTo(200);
    }

    @Test
    void onlyFailedLoginsCountAndTheWindowExpires() throws Exception {
        AbuseLimitFilter filter = new AbuseLimitFilter(clock, new TrustedDevices(clock));
        for (int i = 0; i < 100; i++) {
            send(filter, "POST", "/api/v1/auth/login", "10.0.0.1", 200);
        }
        for (int i = 0; i < 50; i++) {
            assertThat(send(filter, "POST", "/api/v1/auth/login", "10.0.0.1", 401)).isEqualTo(401);
        }
        assertThat(send(filter, "POST", "/api/v1/auth/login", "10.0.0.1", 200)).isEqualTo(429);

        now = now.plus(Duration.ofMinutes(16));
        assertThat(send(filter, "POST", "/api/v1/auth/login", "10.0.0.1", 200)).as("pasó la ventana").isEqualTo(200);
    }

    @Test
    void failedStudentRegistrationsAreLimitedButSuccessfulOnesAreNot() throws Exception {
        AbuseLimitFilter filter = new AbuseLimitFilter(clock, new TrustedDevices(clock));
        for (int i = 0; i < 60; i++) {
            assertThat(send(filter, "POST", "/api/v1/auth/student-registrations", "10.0.0.1", 201)).isEqualTo(201);
        }
        for (int i = 0; i < 30; i++) {
            send(filter, "POST", "/api/v1/auth/student-registrations", "10.0.0.1", 409);
        }
        assertThat(send(filter, "POST", "/api/v1/auth/student-registrations", "10.0.0.1", 201)).isEqualTo(429);
    }

    @Test
    void aKnownDeviceIsNotStoppedByTheSharedAddressLimit() throws Exception {
        TrustedDevices devices = new TrustedDevices(clock);
        AbuseLimitFilter filter = new AbuseLimitFilter(clock, devices);
        for (int i = 0; i < 50; i++) {
            send(filter, "POST", "/api/v1/auth/login", "10.0.0.1", 401);
        }
        assertThat(send(filter, "POST", "/api/v1/auth/login", "10.0.0.1", 200)).isEqualTo(429);
        String cookie = devices.issue(java.util.UUID.randomUUID());
        assertThat(send(filter, "POST", "/api/v1/auth/login", "10.0.0.1", 200, cookie)).isEqualTo(200);
        assertThat(send(filter, "POST", "/api/v1/auth/login", "10.0.0.1", 200, cookie + "x")).isEqualTo(429);
    }

    private static int send(AbuseLimitFilter filter, String method, String uri, String ip, int status)
            throws Exception {
        return send(filter, method, uri, ip, status, null);
    }

    private static int send(AbuseLimitFilter filter, String method, String uri, String ip, int status, String device)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(ip);
        if (device != null) {
            request.setCookies(new jakarta.servlet.http.Cookie(TrustedDevices.COOKIE, device));
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                ((MockHttpServletResponse) res).setStatus(status);
            }
        });
        return response.getStatus();
    }
}
