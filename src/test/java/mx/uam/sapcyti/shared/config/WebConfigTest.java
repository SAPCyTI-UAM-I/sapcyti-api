package mx.uam.sapcyti.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class WebConfigTest {

    @Test
    void addCorsMappingsAcceptsCommaSeparatedOrigins() {
        WebConfig config = new WebConfig();
        ReflectionTestUtils.setField(config, "corsAllowedOrigins", "http://localhost:4200,http://localhost:3000");
        CorsRegistry registry = new CorsRegistry();
        config.addCorsMappings(registry);
    }
}
