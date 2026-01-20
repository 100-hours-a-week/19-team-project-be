package org.refit.refitbackend;

import org.refit.refitbackend.domain.auth.config.properties.OAuth2ProviderProperties;
import org.refit.refitbackend.domain.auth.config.properties.OAuth2RegistrationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableConfigurationProperties({
    OAuth2ProviderProperties.class,
            OAuth2RegistrationProperties.class
})
@SpringBootApplication
public class RefitBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RefitBackendApplication.class, args);
    }

}
