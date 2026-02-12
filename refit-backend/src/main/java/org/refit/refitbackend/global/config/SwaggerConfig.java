package org.refit.refitbackend.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .schemaRequirement("refreshToken", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("Refresh-Token"))
                .schemaRequirement("bearerAuth", bearerAuth)
                .info(new Info()
                        .title("Re-Fit API")
                        .description("현직자-구직자 매칭 및 리포트 플랫폼")
                        .version("v1"));
    }

    @Bean
    public GroupedOpenApi apiV1(OperationCustomizer swaggerApiCustomizer) {
        return GroupedOpenApi.builder()
                .group("v1")
                .pathsToMatch("/api/v1/**")
                .addOperationCustomizer(swaggerApiCustomizer)
                .build();
    }

    @Bean
    public GroupedOpenApi apiV2(OperationCustomizer swaggerApiCustomizer) {
        return GroupedOpenApi.builder()
                .group("v2")
                .pathsToMatch("/api/v2/**")
                .addOperationCustomizer(swaggerApiCustomizer)
                .build();
    }
}
