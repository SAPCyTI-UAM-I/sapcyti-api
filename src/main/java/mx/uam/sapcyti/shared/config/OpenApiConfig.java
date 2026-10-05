package mx.uam.sapcyti.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import mx.uam.sapcyti.shared.tenant.TenantFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI sapcytiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SAPCyTI API")
                        .description("""
                                Sistema de Administración de Posgrado del PCyTI — UAM Iztapalapa.

                                Protected endpoints require a JWT access token (`Authorization: Bearer <token>`).
                                Tenant-scoped endpoints also accept `X-Graduate-Id` when the caller role allows \
                                program selection (see tenant documentation).
                                """)
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes("graduateTenant", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(TenantFilter.HEADER_GRADUATE_ID)
                                .description("Graduate program tenant scope (when applicable)")));
    }
}
