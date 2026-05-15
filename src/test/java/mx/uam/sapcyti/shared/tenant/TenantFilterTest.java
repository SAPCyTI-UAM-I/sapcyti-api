package mx.uam.sapcyti.shared.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TenantFilterTest {

    private final TenantFilter filter = new TenantFilter();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        MDC.clear();
    }

    @Test
    void contextClearedAfterRequestAndResponseHasRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantFilter.HEADER_GRADUATE_ID, "42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (ServletRequest req, ServletResponse res) -> {
        };
        filter.doFilter(request, response, chain);
        assertNull(TenantContext.get());
        assertNull(MDC.get(TenantFilter.MDC_GRADUATE_PROGRAM_ID));
        assertNotNull(response.getHeader(TenantFilter.HEADER_REQUEST_ID));
    }

    @Test
    void graduateIdVisibleInsideChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantFilter.HEADER_GRADUATE_ID, "99");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (ServletRequest req, ServletResponse res) -> {
            assertEquals(99L, TenantContext.get());
            assertEquals("99", MDC.get(TenantFilter.MDC_GRADUATE_PROGRAM_ID));
        };
        filter.doFilter(request, response, chain);
    }

    @Test
    void clientRequestIdIsEchoed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantFilter.HEADER_REQUEST_ID, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (ServletRequest req, ServletResponse res) -> {
        };
        filter.doFilter(request, response, chain);
        assertEquals("abc-123", response.getHeader(TenantFilter.HEADER_REQUEST_ID));
    }
}
