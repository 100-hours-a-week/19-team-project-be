package org.refit.refitbackend.domain.auth.config;

import org.refit.refitbackend.domain.auth.config.properties.OAuth2ProviderProperties;
import org.refit.refitbackend.domain.auth.config.properties.OAuth2RegistrationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        OAuth2ProviderProperties.class,
        OAuth2RegistrationProperties.class
})
public class OAuth2PropertiesConfig {
}
