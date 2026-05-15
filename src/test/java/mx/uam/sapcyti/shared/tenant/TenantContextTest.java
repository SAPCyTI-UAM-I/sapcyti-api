package mx.uam.sapcyti.shared.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void setGetClearRoundTrip() {
        TenantContext.set(7L);
        assertEquals(7L, TenantContext.get());
        TenantContext.clear();
        assertNull(TenantContext.get());
    }
}
